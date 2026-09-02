package com.example.core.agent

import com.example.core.result.ArohiResult
import com.example.core.result.StatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class TaskState { QUEUED, PLANNING, EXECUTING, VERIFYING, SUCCEEDED, FAILED, CANCELLED, TIMEOUT }

/** A single atomic step inside a [AgentTask]. [execute] performs the real action. */
data class AgentStep(
    val name: String,
    /** Returns the real result of the action. Never throws for expected failures. */
    val execute: suspend () -> ArohiResult<String>,
    /** Optional real verification; null means the execute result's [verified] is used. */
    val verify: (suspend () -> ArohiResult<String>)? = null,
    val maxRetries: Int = 2
)

data class AgentTask(
    val id: String,
    val title: String,
    val steps: List<AgentStep>,
    val requiresConfirmation: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskStepResult(
    val name: String,
    val state: TaskState,
    val message: String,
    val attempts: Int
)

/** Observable snapshot of a running/finished task. */
data class TaskSnapshot(
    val id: String,
    val title: String,
    val state: TaskState,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val steps: List<TaskStepResult>,
    val error: String? = null
) {
    val isTerminal: Boolean get() = state in setOf(TaskState.SUCCEEDED, TaskState.FAILED, TaskState.CANCELLED, TaskState.TIMEOUT)
    val progress: Float get() = if (totalSteps == 0) 0f else currentStepIndex.toFloat() / totalSteps
}

/**
 * The autonomous task agent (spec §8). It runs a planned list of real steps with:
 *  - bounded retry with exponential back-off (never an infinite loop),
 *  - per-step timeout,
 *  - real verification where a verifier is provided,
 *  - cancellation, progress reporting and execution history.
 *
 * It is free of Android dependencies so the whole lifecycle is unit-testable.
 */
class TaskAgent(
    private val scope: CoroutineScope,
    private val stepTimeoutMs: Long = 30_000L
) {
    private val jobs = mutableMapOf<String, Job>()

    private val _tasks = MutableStateFlow<Map<String, TaskSnapshot>>(emptyMap())
    val tasks: StateFlow<Map<String, TaskSnapshot>> = _tasks.asStateFlow()

    private val _history = MutableStateFlow<List<TaskSnapshot>>(emptyList())
    val history: StateFlow<List<TaskSnapshot>> = _history.asStateFlow()

    fun currentTask(id: String): TaskSnapshot? = _tasks.value[id]

    fun enqueue(task: AgentTask): TaskSnapshot {
        val initial = TaskSnapshot(
            id = task.id,
            title = task.title,
            state = TaskState.QUEUED,
            currentStepIndex = 0,
            totalSteps = task.steps.size,
            steps = task.steps.map { TaskStepResult(it.name, TaskState.QUEUED, "", 0) }
        )
        update(initial)
        return initial
    }

    fun cancel(taskId: String) {
        jobs[taskId]?.cancel()
        _tasks.value[taskId]?.let { snap ->
            update(snap.copy(state = TaskState.CANCELLED, error = "Cancelled by user"))
        }
    }

    fun run(task: AgentTask, onFinished: (TaskSnapshot) -> Unit = {}) {
        enqueue(task)
        val job = scope.launch {
            var current = 0
            val stepResults = task.steps.map { TaskStepResult(it.name, TaskState.QUEUED, "", 0) }.toMutableList()

            fun snap(state: TaskState, error: String? = null) = TaskSnapshot(
                id = task.id, title = task.title, state = state,
                currentStepIndex = current, totalSteps = task.steps.size,
                steps = stepResults.toList(), error = error
            )

            setState(task.id, snap(TaskState.EXECUTING))

            for ((index, step) in task.steps.withIndex()) {
                current = index
                if (!jobs.containsKey(task.id)) return@launch
                var attempts = 0
                var lastMessage = ""
                var stepState = TaskState.EXECUTING

                while (attempts <= step.maxRetries) {
                    attempts++
                    stepResults[index] = TaskStepResult(step.name, TaskState.EXECUTING, "Attempt $attempts…", attempts)
                    setState(task.id, snap(TaskState.EXECUTING))

                    val execResult = withTimeoutOrNull(stepTimeoutMs) {
                        try { step.execute() } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { ArohiResult.failed(com.example.core.result.ArohiErrorCode.UNKNOWN, technicalCause = e.message) }
                    }

                    if (execResult == null) {
                        stepResults[index] = TaskStepResult(step.name, TaskState.TIMEOUT, "Step timed out", attempts)
                        setState(task.id, snap(TaskState.TIMEOUT, "Step '${step.name}' timed out"))
                        finish(task.id, snap(TaskState.TIMEOUT, "Step '${step.name}' timed out"), onFinished)
                        return@launch
                    }

                    if (execResult.status == StatusCode.REQUIRES_CONFIRMATION) {
                        stepResults[index] = TaskStepResult(step.name, TaskState.FAILED, execResult.message, attempts)
                        setState(task.id, snap(TaskState.FAILED, execResult.message))
                        finish(task.id, snap(TaskState.FAILED, execResult.message), onFinished)
                        return@launch
                    }

                    if (execResult.succeeded) {
                        // Real verification when provided.
                        val verified = if (step.verify != null) {
                            setState(task.id, snap(TaskState.VERIFYING))
                            try { step.verify.invoke() } catch (e: Exception) {
                                ArohiResult.failed(com.example.core.result.ArohiErrorCode.UNKNOWN, technicalCause = e.message)
                            }
                        } else execResult

                        if (verified.succeeded) {
                            stepState = TaskState.SUCCEEDED
                            lastMessage = verified.message
                            stepResults[index] = TaskStepResult(step.name, TaskState.SUCCEEDED, verified.message, attempts)
                            break
                        }
                        lastMessage = "Verification failed: ${verified.message}"
                    } else {
                        lastMessage = execResult.message
                    }

                    if (attempts <= step.maxRetries) {
                        // Exponential back-off: 0.5s, 1s, 2s… bounded.
                        delay(500L * (1 shl (attempts - 1)))
                    }
                }

                if (stepState != TaskState.SUCCEEDED) {
                    stepResults[index] = TaskStepResult(step.name, TaskState.FAILED, lastMessage, attempts)
                    val failSnap = snap(TaskState.FAILED, "Step '${step.name}' failed: $lastMessage")
                    setState(task.id, failSnap)
                    finish(task.id, failSnap, onFinished)
                    return@launch
                }
            }

            val done = snap(TaskState.SUCCEEDED).copy(currentStepIndex = task.steps.size)
            setState(task.id, done)
            finish(task.id, done, onFinished)
        }
        jobs[task.id] = job
    }

    private fun finish(id: String, snapshot: TaskSnapshot, onFinished: (TaskSnapshot) -> Unit) {
        jobs.remove(id)
        _history.value = (_history.value.filterNot { it.id == snapshot.id } + snapshot).takeLast(50)
        onFinished(snapshot)
    }

    private fun setState(id: String, snapshot: TaskSnapshot) {
        _tasks.value = _tasks.value + (id to snapshot)
    }

    private fun update(snapshot: TaskSnapshot) {
        _tasks.value = _tasks.value + (snapshot.id to snapshot)
    }
}

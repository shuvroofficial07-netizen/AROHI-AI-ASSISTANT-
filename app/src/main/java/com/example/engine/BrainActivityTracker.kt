package com.example.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real, observable stages of AROHI's request pipeline.
 *
 * This tracks the ACTUAL execution pipeline (understand -> context -> plan ->
 * execute -> verify), never the private chain-of-thought of the Gemini model.
 */
enum class BrainPhase(val label: String) {
    IDLE("IDLE"),
    UNDERSTANDING("UNDERSTANDING"),
    CHECKING_CONTEXT("CHECKING CONTEXT"),
    PLANNING_ACTION("PLANNING ACTION"),
    EXECUTING("EXECUTING"),
    VERIFYING("VERIFYING"),
    RESPONDING("RESPONDING"),
    DONE("DONE"),
    ERROR("ERROR")
}

/** One verified step of a multi-step task, recorded from the real tool pipeline. */
data class TaskStep(
    val order: Int,
    val toolName: String,
    val description: String,
    val status: TaskStepStatus,
    val detail: String = ""
)

enum class TaskStepStatus { PENDING, RUNNING, COMPLETED, FAILED }

/** Live task progress shown by the Task Execution UI. */
data class TaskProgress(
    val isRunning: Boolean = false,
    val taskName: String = "",
    val steps: List<TaskStep> = emptyList(),
    val overallStatus: TaskStepStatus = TaskStepStatus.PENDING
)

/**
 * App-scoped holder for the live brain phase + task progress.
 * Instantiated once in [com.example.ArohiApplication] so every screen
 * observes the same real pipeline state.
 */
class BrainActivityTracker {
    private val _phase = MutableStateFlow(BrainPhase.IDLE)
    val phase: StateFlow<BrainPhase> = _phase.asStateFlow()

    private val _task = MutableStateFlow(TaskProgress())
    val task: StateFlow<TaskProgress> = _task.asStateFlow()

    fun setPhase(phase: BrainPhase) {
        _phase.value = phase
    }

    fun resetTask() {
        _task.value = TaskProgress()
    }

    fun startTask(taskName: String) {
        _task.value = TaskProgress(isRunning = true, taskName = taskName, overallStatus = TaskStepStatus.RUNNING)
    }

    fun setSteps(steps: List<TaskStep>) {
        _task.value = _task.value.copy(steps = steps)
    }

    fun finishTask(success: Boolean, summary: String = "") {
        _task.value = _task.value.copy(
            isRunning = false,
            overallStatus = if (success) TaskStepStatus.COMPLETED else TaskStepStatus.FAILED
        )
    }
}

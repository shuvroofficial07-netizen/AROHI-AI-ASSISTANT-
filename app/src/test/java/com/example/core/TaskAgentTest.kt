package com.example.core

import com.example.core.agent.AgentStep
import com.example.core.agent.AgentTask
import com.example.core.agent.TaskAgent
import com.example.core.agent.TaskState
import com.example.core.result.ArohiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskAgentTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun `runs all steps and succeeds`() = runBlocking {
        val agent = TaskAgent(scope)
        val order = mutableListOf<String>()
        val task = AgentTask(
            id = "t1",
            title = "Prepare meeting",
            steps = listOf(
                AgentStep("calendar") { order += "cal"; ArohiResult.success("calendar checked") },
                AgentStep("notes") { order += "notes"; ArohiResult.success("notes summarized") }
            )
        )
        var finished: TaskState? = null
        agent.run(task) { finished = it.state }
        // Unconfined dispatcher runs eagerly; allow completion.
        repeat(10) { delay(10) }
        assertEquals(listOf("cal", "notes"), order)
        assertEquals(TaskState.SUCCEEDED, finished)
    }

    @Test
    fun `retries failing step then succeeds`() = runBlocking {
        val agent = TaskAgent(scope)
        var attempts = 0
        val step = AgentStep(
            name = "flaky",
            maxRetries = 2,
            execute = {
                attempts++
                if (attempts < 2) ArohiResult.failed(com.example.core.result.ArohiErrorCode.UNKNOWN, "boom")
                else ArohiResult.success("ok")
            }
        )
        var finished: TaskState? = null
        agent.run(AgentTask(id = "t2", title = "flaky", steps = listOf(step))) { finished = it.state }
        repeat(20) { delay(20) }
        assertEquals(2, attempts)
        assertEquals(TaskState.SUCCEEDED, finished)
    }

    @Test
    fun `permanently failing step marks task failed`() = runBlocking {
        val agent = TaskAgent(scope)
        val step = AgentStep("bad", maxRetries = 1) {
            ArohiResult.failed(com.example.core.result.ArohiErrorCode.UNKNOWN, "always fails")
        }
        var finished: TaskState? = null
        agent.run(AgentTask(id = "t3", title = "bad", steps = listOf(step))) { finished = it.state }
        repeat(20) { delay(20) }
        assertEquals(TaskState.FAILED, finished)
    }

    @Test
    fun `cancellation stops task`() = runBlocking {
        val agent = TaskAgent(scope)
        val step = AgentStep("long") {
            delay(5000)
            ArohiResult.success("done")
        }
        val snap = agent.enqueue(AgentTask(id = "t4", title = "long", steps = listOf(step)))
        agent.cancel("t4")
        val state = agent.currentTask("t4")?.state
        assertEquals(TaskState.CANCELLED, state)
        assertTrue(snap.totalSteps == 1)
    }
}

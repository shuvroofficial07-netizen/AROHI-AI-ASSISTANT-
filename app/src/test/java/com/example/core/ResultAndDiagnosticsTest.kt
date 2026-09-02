package com.example.core

import com.example.core.diagnostics.DiagnosticCheckResult
import com.example.core.diagnostics.DiagnosticComponent
import com.example.core.diagnostics.DiagnosticReport
import com.example.core.diagnostics.DiagnosticStatus
import com.example.core.result.ArohiErrorCode
import com.example.core.result.ArohiResult
import com.example.core.result.StatusCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultAndDiagnosticsTest {

    @Test
    fun `success result is verified and ok`() {
        val r = ArohiResult.success("done", message = "ok", verified = true)
        assertTrue(r.succeeded)
        assertEquals(StatusCode.SUCCESS, r.status)
        assertTrue(r.verified)
    }

    @Test
    fun `failed result carries error code and recovery`() {
        val r = ArohiResult.failed<Unit>(ArohiErrorCode.MIC_PERMISSION_DENIED)
        assertFalse(r.succeeded)
        assertEquals(StatusCode.FAILED, r.status)
        assertEquals(ArohiErrorCode.MIC_PERMISSION_DENIED, r.errorCode)
        assertNotNull(r.recoveryAction)
    }

    @Test
    fun `requires permission status`() {
        val r = ArohiResult.requiresPermission<Unit>(ArohiErrorCode.ACCESSIBILITY_DISABLED)
        assertTrue(r.needsPermission)
        assertEquals(StatusCode.REQUIRES_PERMISSION, r.status)
    }

    @Test
    fun `unsupported never reports success`() {
        val r = ArohiResult.unsupported<Unit>("No flash unit")
        assertFalse(r.succeeded)
        assertEquals(StatusCode.UNSUPPORTED, r.status)
    }

    @Test
    fun `diagnostic report counts statuses`() {
        val report = DiagnosticReport(
            listOf(
                DiagnosticCheckResult(DiagnosticComponent.CORE, DiagnosticStatus.PASS, "ok"),
                DiagnosticCheckResult(DiagnosticComponent.DATABASE, DiagnosticStatus.PASS, "ok"),
                DiagnosticCheckResult(DiagnosticComponent.AI, DiagnosticStatus.WARNING, "no key"),
                DiagnosticCheckResult(DiagnosticComponent.STORAGE, DiagnosticStatus.NOT_AVAILABLE, "n/a")
            )
        )
        assertEquals(2, report.pass.size)
        assertEquals(1, report.warnings.size)
        assertEquals(1, report.notAvailable.size)
        assertEquals(0, report.failed.size)
        assertEquals(DiagnosticStatus.WARNING, report.overall)
        assertTrue(report.summary().contains("PASS 2"))
    }

    @Test
    fun `critical failure makes overall failed`() {
        val report = DiagnosticReport(
            listOf(
                DiagnosticCheckResult(DiagnosticComponent.CORE, DiagnosticStatus.PASS, "ok"),
                DiagnosticCheckResult(DiagnosticComponent.DATABASE, DiagnosticStatus.FAILED, "db broken")
            )
        )
        assertEquals(DiagnosticStatus.FAILED, report.overall)
    }

    @Test
    fun `render text includes each component`() {
        val report = DiagnosticReport(
            listOf(DiagnosticCheckResult(DiagnosticComponent.MICROPHONE, DiagnosticStatus.PASS, "mic ok"))
        )
        val text = report.renderText()
        assertTrue(text.contains("Microphone"))
        assertTrue(text.contains("PASS"))
    }
}

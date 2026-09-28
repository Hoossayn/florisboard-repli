package dev.patrickgold.florisboard.repli.capture

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReplyScreenCaptureServiceTest {
    @Test
    fun completedCaptureIsNotReportedAsInterruptedWhileAiReads() {
        assertFalse(shouldReportCaptureInterruption(resultDelivered = true, ReplyPhase.READING))
    }

    @Test
    fun unfinishedCaptureIsReportedAsInterrupted() {
        assertTrue(shouldReportCaptureInterruption(resultDelivered = false, ReplyPhase.RETURNING))
        assertTrue(shouldReportCaptureInterruption(resultDelivered = false, ReplyPhase.READING))
    }

    @Test
    fun finishedOrInactivePhasesAreNotReportedAsInterrupted() {
        assertFalse(shouldReportCaptureInterruption(resultDelivered = false, ReplyPhase.CAPTURE_REVIEW))
        assertFalse(shouldReportCaptureInterruption(resultDelivered = false, ReplyPhase.CONTEXT))
        assertFalse(shouldReportCaptureInterruption(resultDelivered = false, ReplyPhase.ERROR))
        assertFalse(shouldReportCaptureInterruption(resultDelivered = false, null))
    }
}

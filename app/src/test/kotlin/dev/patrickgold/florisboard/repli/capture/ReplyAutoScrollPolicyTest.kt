package dev.patrickgold.florisboard.repli.capture

import kotlin.test.Test
import kotlin.test.assertEquals

class ReplyManualCapturePolicyTest {
    @Test fun `only a blank chat stops after the first frame`() {
        assertEquals(
            ManualCaptureDecision.SHORT_CHAT,
            ReplyManualCapturePolicy.afterFrame(1, 0, 0),
        )
        for (turns in 1..3) assertEquals(
            ManualCaptureDecision.CONTINUE,
            ReplyManualCapturePolicy.afterFrame(1, turns, 0),
        )
    }

    @Test fun `longer first frame opens the manual scrolling window`() {
        assertEquals(ManualCaptureDecision.CONTINUE, ReplyManualCapturePolicy.afterFrame(1, 4, 0))
    }

    @Test fun `explicit page capture remains bounded by time and frame count`() {
        assertEquals(ManualCaptureDecision.CONTINUE, ReplyManualCapturePolicy.afterFrame(2, 10, 60_000))
        assertEquals(ManualCaptureDecision.TIME_LIMIT, ReplyManualCapturePolicy.afterFrame(2, 10, 180_000))
        assertEquals(ManualCaptureDecision.FRAME_LIMIT, ReplyManualCapturePolicy.afterFrame(4, 15, 6_000))
    }

    @Test fun `visual difference ignores equal frames and detects moved content`() {
        assertEquals(0, visualDifference(intArrayOf(10, 20, 30), intArrayOf(10, 20, 30)))
        assertEquals(10, visualDifference(intArrayOf(10, 20, 30), intArrayOf(20, 30, 40)))
    }
}

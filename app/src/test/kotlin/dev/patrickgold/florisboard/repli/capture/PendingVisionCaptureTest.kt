package dev.patrickgold.florisboard.repli.capture

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PendingVisionCaptureTest {
    @Test fun `replacement and discard overwrite pending image bytes`() {
        val first = byteArrayOf(1, 2, 3)
        val second = byteArrayOf(4, 5, 6)
        PendingVisionCaptureStore.put(PendingVisionCapture("first", listOf(first), emptyList(), emptyList()))
        PendingVisionCaptureStore.put(PendingVisionCapture("second", listOf(second), emptyList(), emptyList()))
        assertContentEquals(byteArrayOf(0, 0, 0), first)

        PendingVisionCaptureStore.discard("second")
        assertContentEquals(byteArrayOf(0, 0, 0), second)
        assertNull(PendingVisionCaptureStore.peek("second"))
    }

    @Test fun `take transfers byte ownership without copying or early overwrite`() {
        val bytes = byteArrayOf(7, 8, 9)
        PendingVisionCaptureStore.put(PendingVisionCapture("request", listOf(bytes), emptyList(), emptyList()))

        val taken = PendingVisionCaptureStore.take("request")

        assertSame(bytes, taken?.images?.single())
        assertContentEquals(byteArrayOf(7, 8, 9), bytes)
        assertNull(PendingVisionCaptureStore.peek("request"))
        bytes.fill(0)
    }
}

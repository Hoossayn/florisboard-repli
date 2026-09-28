package dev.patrickgold.florisboard.repli.review

import dev.patrickgold.florisboard.repli.capture.ReplyEditor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FullScreenContextReviewSessionTest {
    private val requestId = "review-request"
    private val original = ReplyEditor("chat.app", 42, "composer")
    private val reviewEditor = ReplyEditor("repli.app", 0, "correction")

    @AfterTest fun clear() = FullScreenContextReviewSession.end(requestId)

    @Test fun `review editor never replaces the host chat editor on return`() {
        FullScreenContextReviewSession.begin(requestId)
        assertFalse(FullScreenContextReviewSession.acceptsEditor(requestId, original, reviewEditor, "repli.app"))
        FullScreenContextReviewSession.markReturning(requestId)
        assertFalse(FullScreenContextReviewSession.acceptsEditor(requestId, original, reviewEditor, "repli.app"))
        assertTrue(FullScreenContextReviewSession.acceptsEditor(
            requestId, original, original.copy(fieldId = 43), "repli.app"))
    }

    @Test fun `another chat cannot claim a review return`() {
        FullScreenContextReviewSession.begin(requestId)
        FullScreenContextReviewSession.markReturning(requestId)
        assertFalse(FullScreenContextReviewSession.acceptsEditor(
            requestId, original, ReplyEditor("other.app", 42, "composer"), "repli.app"))
    }
}

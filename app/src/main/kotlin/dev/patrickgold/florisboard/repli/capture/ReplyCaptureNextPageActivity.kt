package dev.patrickgold.florisboard.repli.capture

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Notification actions open an Activity so Android closes the shade before the next frame. */
class ReplyCaptureNextPageActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestId = intent.getStringExtra(ReplyScreenCaptureService.EXTRA_REQUEST_ID).orEmpty()
        if (requestId.isNotBlank() && ReplyCaptureSession.state.value?.id == requestId) {
            startService(ReplyScreenCaptureService.nextViewIntent(this, requestId))
        }
        finish()
    }
}

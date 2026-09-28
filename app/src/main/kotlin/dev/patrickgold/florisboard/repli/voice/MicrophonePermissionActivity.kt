package dev.patrickgold.florisboard.repli.voice

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import dev.patrickgold.florisboard.repli.capture.ReplyCaptureSession
import dev.patrickgold.florisboard.repli.capture.ReplyPhase

/** Translucent permission host only. Recording and text stay in the IME, not this activity. */
class MicrophonePermissionActivity : ComponentActivity() {
    private var requestId: String? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        requestId?.let { ReplyCaptureSession.returnFromMicrophonePermission(it, granted) }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID)
        val state = ReplyCaptureSession.state.value
        if (state?.id != requestId || state?.phase != ReplyPhase.MICROPHONE_PERMISSION) { finish(); return }
        if (savedInstanceState == null) permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onDestroy() {
        if (isFinishing) requestId?.let { ReplyCaptureSession.returnFromMicrophonePermission(it, false) }
        super.onDestroy()
    }

    companion object { const val EXTRA_REQUEST_ID = "microphone_request_id" }
}

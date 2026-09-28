package dev.patrickgold.florisboard.repli.review

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import dev.patrickgold.florisboard.repli.capture.ReplyCaptureSession
import dev.patrickgold.florisboard.repli.capture.ReplyPhase
import dev.patrickgold.florisboard.repli.suggestions.RemoteReplyPrivacyPolicy

/** Focused direction editor. Text typed here belongs to Repli, never the host chat. */
class ReplyGuidanceActivity : Activity() {
    private var requestId = ""
    private var fromReview = false
    private lateinit var direction: EditText
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        fromReview = intent.getBooleanExtra(EXTRA_FROM_REVIEW, false)
        val state = ReplyCaptureSession.state.value
        if (requestId.isBlank() || state?.id != requestId || state.phase != ReplyPhase.DRAFT) {
            finish()
            return
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = ACCENT
        window.navigationBarColor = PAPER
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(18))
            setBackgroundColor(PAPER)
        }
        root.addView(label("Guide this reply", 24f, true))
        root.addView(label("Add context or describe how you want to respond. You'll review the chat before generating.", 14f),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        direction = EditText(this).apply {
            setText(savedInstanceState?.getString("direction") ?: state.instructions.orEmpty())
            hint = "For example: decline politely and suggest another day"
            textSize = 16f
            setTextColor(INK)
            setHintTextColor(MUTED)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
            maxLines = 6
            filters = arrayOf(InputFilter.LengthFilter(RemoteReplyPrivacyPolicy.MAX_INSTRUCTION_CHARACTERS))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), LINE)
            }
            setSelection(text.length)
        }
        root.addView(direction, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })
        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
        footer.addView(Button(this).apply {
            text = "Cancel"
            setOnClickListener { complete(save = false) }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        footer.addView(Button(this).apply {
            text = "Use direction"
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ACCENT)
            setOnClickListener { complete(save = true) }
        }, LinearLayout.LayoutParams(0, dp(52), 2f).apply { marginStart = dp(8) })
        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(footer)
        setContentView(root)
        direction.requestFocus()
        direction.postDelayed({ getSystemService(InputMethodManager::class.java).showSoftInput(direction, InputMethodManager.SHOW_IMPLICIT) }, 160)
    }

    private fun label(value: String, size: Float, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(if (bold) INK else MUTED)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun complete(save: Boolean) {
        val state = ReplyCaptureSession.state.value
        if (state?.id == requestId && state.phase == ReplyPhase.DRAFT) {
            ReplyCaptureSession.finishGuidance(
                requestId,
                if (save) direction.text.toString() else state.instructions,
                reviewBeforeGenerate = fromReview,
            )
        }
        FullScreenContextReviewSession.markReturning(requestId)
        finish()
    }

    override fun onBackPressed() = complete(save = false)

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("direction", direction.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && !FullScreenContextReviewSession.isReturning(requestId)) {
            FullScreenContextReviewSession.end(requestId)
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_REQUEST_ID = "request_id"
        private const val EXTRA_FROM_REVIEW = "from_review"
        private val PAPER = Color.rgb(243, 240, 250)
        private val INK = Color.rgb(39, 36, 58)
        private val MUTED = Color.rgb(110, 106, 128)
        private val ACCENT = Color.rgb(102, 84, 209)
        private val LINE = Color.rgb(228, 222, 238)

        fun intent(context: Context, requestId: String, fromReview: Boolean) =
            Intent(context, ReplyGuidanceActivity::class.java)
                .putExtra(EXTRA_REQUEST_ID, requestId)
                .putExtra(EXTRA_FROM_REVIEW, fromReview)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
}

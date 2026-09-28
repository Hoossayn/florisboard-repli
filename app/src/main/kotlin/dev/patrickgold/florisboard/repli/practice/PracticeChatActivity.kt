package dev.patrickgold.florisboard.repli.practice

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Real editable chat surface with clearly labelled sample content. Nothing sends. */
class PracticeChatActivity : Activity() {
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PAPER)
        }
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(10))
            addView(title("Alex · practice chat"))
            addView(subtitle("Sample messages · nothing is sent"))
        })
        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        fun bubble(text: String, fromMe: Boolean) {
            messages.addView(TextView(this).apply {
                this.text = text
                textSize = 17f
                setTextColor(INK)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(if (fromMe) ACCENT_SOFT else Color.WHITE)
                maxWidth = dp(260)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = if (fromMe) Gravity.END else Gravity.START
                topMargin = dp(10)
            })
        }
        bubble("Hey! How is your day going?", true)
        bubble("Pretty good, thanks! Want to grab coffee tomorrow?", false)
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(messages) }
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(EditText(this).apply {
            id = android.R.id.edit
            hint = "Type here, or try the reply icon"
            textSize = 16f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(Color.WHITE)
            setOnFocusChangeListener { _, focused -> if (focused) scroll.postDelayed({ scroll.fullScroll(View.FOCUS_DOWN) }, 450) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(12), dp(6), dp(12), dp(6))
        })
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom.coerceAtMost(dp(48)))
            }
            insets
        }
        setContentView(root)
        root.requestApplyInsets()
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(INK)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(MUTED)
    }

    private fun rounded(color: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(16).toFloat()
    }

    private companion object {
        const val PAPER = 0xFFFAF8F5.toInt()
        const val INK = 0xFF27243A.toInt()
        const val MUTED = 0xFF6E6A80.toInt()
        const val ACCENT_SOFT = 0xFFEEEAFE.toInt()
    }
}

package dev.patrickgold.florisboard.app.settings.repli

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.patrickgold.florisboard.lib.compose.LocalPreviewFieldController

internal object RepliStyle {
    val ink: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF4F1FA) else Color(0xFF27243A)
    val muted: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFB8B1C6) else Color(0xFF6E6A80)
    val paper: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF17151E) else Color(0xFFFAF8F5)
    val card: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF25212F) else Color.White
    val accent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFAB9BFF) else Color(0xFF6654D1)
    val onAccent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF21183E) else Color.White
    val accentSoft: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF383050) else Color(0xFFEEEAFE)
    val line: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF453E52) else Color(0xFFE7E2EE)
    val strongLine: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF655A73) else Color(0xFFD7CEE5)
}

@Composable
internal fun RepliPage(content: @Composable () -> Unit) {
    val window = (LocalContext.current as Activity).window
    val paper = RepliStyle.paper
    val dark = isSystemInDarkTheme()
    val previewFieldController = LocalPreviewFieldController.current
    SideEffect {
        previewFieldController?.isVisible = false
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        window.statusBarColor = paper.toArgb()
        window.navigationBarColor = paper.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
    Column(Modifier.fillMaxSize().background(paper).statusBarsPadding()) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RepliLabel("repli", 30, RepliStyle.ink, bold = true)
            RepliLabel("Your words. Your call.", 12, RepliStyle.muted,
                modifier = Modifier.padding(top = 14.dp))
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 28.dp),
        ) { content() }
    }
}

@Composable
internal fun RepliLabel(text: String, size: Int, color: Color, bold: Boolean = false,
    modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = color, fontSize = size.sp,
        lineHeight = (size + 3).sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
}

@Composable
internal fun RepliCard(tinted: Boolean = false, modifier: Modifier = Modifier,
    content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (tinted) RepliStyle.accentSoft else RepliStyle.card,
        border = if (tinted) null else BorderStroke(1.dp, RepliStyle.line),
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
internal fun RepliAction(label: String, onClick: () -> Unit, filled: Boolean = true,
    enabled: Boolean = true, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = if (filled) null else BorderStroke(1.dp, RepliStyle.strongLine),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) RepliStyle.accent else RepliStyle.card,
            contentColor = if (filled) RepliStyle.onAccent else RepliStyle.accent,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        Text(label, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun RepliSection(title: String, modifier: Modifier = Modifier) {
    RepliLabel(title, 19, RepliStyle.ink, bold = true, modifier = modifier)
    Spacer(Modifier.height(10.dp))
}

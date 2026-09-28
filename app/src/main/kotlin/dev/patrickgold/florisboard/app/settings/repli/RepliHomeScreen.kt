package dev.patrickgold.florisboard.app.settings.repli

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import dev.patrickgold.florisboard.repli.practice.PracticeChatActivity
import org.florisboard.lib.compose.FlorisErrorCard
import org.florisboard.lib.compose.FlorisWarningCard
import org.florisboard.lib.compose.stringRes

private val RepliPaper = Color(0xFFFAF8F5)
private val RepliInk = Color(0xFF27243A)
private val RepliMuted = Color(0xFF6E6A80)
private val RepliAccent = Color(0xFF6654D1)
private val RepliAccentSoft = Color(0xFFEEEAFE)

@Composable
fun RepliHomeScreen() = FlorisScreen {
    title = stringRes(R.string.repli_home__brand)
    navigationIconVisible = false
    previewFieldVisible = false

    val navController = LocalNavController.current
    val context = LocalContext.current

    content {
        val isEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
        val isSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)

        Text(
            text = stringRes(R.string.repli_home__tagline),
            color = RepliMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = RepliAccentSoft),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = stringRes(R.string.repli_home__eyebrow),
                    color = RepliAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringRes(R.string.repli_home__headline),
                    color = RepliInk,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringRes(R.string.repli_home__sub),
                    color = RepliMuted,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { context.startActivity(Intent(context, PracticeChatActivity::class.java)) },
                    colors = ButtonDefaults.buttonColors(containerColor = RepliAccent),
                ) {
                    Text(stringRes(R.string.repli_home__practice))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringRes(R.string.repli_home__practice_caption),
                    color = RepliMuted,
                    fontSize = 12.sp,
                )
            }
        }

        Text(
            text = stringRes(R.string.repli_home__setup_title),
            color = RepliInk,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (!isEnabled) {
            FlorisErrorCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                showIcon = false,
                text = stringRes(R.string.settings__home__ime_not_enabled),
                onClick = { InputMethodUtils.showImeEnablerActivity(context) },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { InputMethodUtils.showImeEnablerActivity(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RepliAccent),
            ) {
                Text(stringRes(R.string.repli_home__enable_keyboard))
            }
        } else if (!isSelected) {
            FlorisWarningCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                showIcon = false,
                text = stringRes(R.string.settings__home__ime_not_selected),
                onClick = { InputMethodUtils.showImePicker(context) },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { InputMethodUtils.showImePicker(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RepliAccent),
            ) {
                Text(stringRes(R.string.repli_home__choose_keyboard))
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringRes(R.string.repli_home__ready),
                    color = RepliInk,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringRes(R.string.repli_home__flow_title),
            color = RepliInk,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(text = stringRes(R.string.repli_home__flow_body), color = RepliMuted)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { navController.navigate(Routes.Settings.RepliReplies) }) {
                    Text(stringRes(R.string.repli_home__flow_action))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringRes(R.string.repli_home__tone_title),
                    color = RepliInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(text = stringRes(R.string.repli_home__tone_body), color = RepliMuted)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = { navController.navigate(Routes.Settings.RepliChats) }) {
                    Text(stringRes(R.string.repli_home__tone_action))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

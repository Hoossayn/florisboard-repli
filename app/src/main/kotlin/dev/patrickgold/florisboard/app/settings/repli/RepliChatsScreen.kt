package dev.patrickgold.florisboard.app.settings.repli

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.repli.data.ProfileRepository
import dev.patrickgold.florisboard.repli.profile.VoiceProfile
import dev.patrickgold.florisboard.repli.profile.VoiceStyle
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import org.florisboard.lib.compose.stringRes

@Composable
fun RepliChatsScreen() = FlorisScreen {
    title = stringRes(R.string.repli_chats__title)
    previewFieldVisible = false

    content {
        val context = LocalContext.current
        var profiles by remember { mutableStateOf(loadProfiles(context)) }
        var newName by remember { mutableStateOf("") }
        var newStyle by remember { mutableStateOf(VoiceStyle.CASUAL) }
        fun refresh() {
            profiles = loadProfiles(context)
        }

        PreferenceGroup(title = stringRes(R.string.repli_chats__saved_title)) {
            if (profiles.isEmpty()) {
                Preference(
                    icon = Icons.Default.Chat,
                    title = stringRes(R.string.repli_chats__empty_title),
                    summary = stringRes(R.string.repli_chats__empty_summary),
                    onClick = { },
                )
            }
            profiles.forEach { profile ->
                Preference(
                    icon = Icons.Default.Chat,
                    title = profile.name,
                    summary = profile.style.displayName,
                    onClick = {
                        ProfileRepository(context).updateStyle(profile.id, profile.style.next())
                        refresh()
                    },
                )
            }
        }

        PreferenceGroup(title = stringRes(R.string.repli_chats__add_title)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringRes(R.string.repli_chats__name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VoiceStyle.entries.forEach { style ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = newStyle == style,
                                onClick = { newStyle = style },
                            )
                            Text(style.displayName)
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            ProfileRepository(context).add(newName, "Other", newStyle)
                            newName = ""
                            refresh()
                        }
                    },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringRes(R.string.repli_chats__add_action))
                }
            }
        }

        if (profiles.isNotEmpty()) {
            PreferenceGroup(title = stringRes(R.string.repli_chats__manage_title)) {
                profiles.forEach { profile ->
                    Preference(
                        icon = Icons.Default.Delete,
                        title = stringRes(R.string.repli_chats__remove_title, "name" to profile.name),
                        onClick = {
                            ProfileRepository(context).remove(profile.id)
                            refresh()
                        },
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringRes(R.string.repli_chats__hint))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringRes(R.string.repli_replies__notification_access))
            }
        }
    }
}

private fun loadProfiles(context: Context): List<VoiceProfile> =
    ProfileRepository(context).profiles()

private fun VoiceStyle.next(): VoiceStyle = when (this) {
    VoiceStyle.CASUAL -> VoiceStyle.WARM
    VoiceStyle.WARM -> VoiceStyle.DIRECT
    VoiceStyle.DIRECT -> VoiceStyle.CASUAL
}

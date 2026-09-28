package dev.patrickgold.florisboard.app.settings.repli

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.repli.data.LearnedStyleRepository
import dev.patrickgold.florisboard.repli.data.ProfileRepository
import dev.patrickgold.florisboard.repli.profile.VoiceProfile
import dev.patrickgold.florisboard.repli.profile.VoiceStyle

@Composable
fun RepliChatsScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val repository = remember(context) { ProfileRepository(context) }
    val learned = remember(context) { LearnedStyleRepository(context) }
    var profiles by remember { mutableStateOf(repository.profiles()) }
    var styleRevision by remember { mutableIntStateOf(0) }
    var addDialog by remember { mutableStateOf(false) }
    var removeProfile by remember { mutableStateOf<VoiceProfile?>(null) }
    var toneProfile by remember { mutableStateOf<VoiceProfile?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                profiles = repository.profiles()
                styleRevision++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    RepliPage {
        RepliLabel("Your chats", 27, RepliStyle.ink, bold = true)
        Spacer(Modifier.height(6.dp))
        RepliLabel("Keep the tone. Skip the setup.", 15, RepliStyle.muted)
        Spacer(Modifier.height(20.dp))
        RepliAction("Add a chat", { addDialog = true })
        Spacer(Modifier.height(16.dp))
        if (profiles.isEmpty()) {
            RepliCard {
                RepliLabel("No saved chats yet", 19, RepliStyle.ink, bold = true)
                Spacer(Modifier.height(8.dp))
                RepliLabel("Start here with a name and a tone, or tap Save in the keyboard when a recent message identifies the person. You can get replies without saving a chat, too.",
                    14, RepliStyle.muted)
            }
        } else {
            profiles.forEach { profile ->
                val style = remember(profile.id, styleRevision) { learned.getStyle(profile.id) }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    color = RepliStyle.card, border = BorderStroke(1.dp, RepliStyle.line)) {
                    Column(Modifier.padding(start = 14.dp, top = 12.dp, end = 10.dp, bottom = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                RepliLabel(profile.name, 16, RepliStyle.ink, bold = true)
                                RepliLabel(if (style == null) "${profile.style.displayName} tone"
                                    else "${profile.style.displayName} · ${style.messagesAnalyzed} writing examples",
                                    13, RepliStyle.muted)
                            }
                            TextButton(onClick = { removeProfile = profile }) {
                                RepliLabel("Remove", 14, RepliStyle.accent)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        RepliAction("Change tone", { toneProfile = profile }, filled = false)
                        if (style != null) {
                            Spacer(Modifier.height(10.dp))
                            RepliLabel(style.summary(), 13, RepliStyle.ink)
                            Spacer(Modifier.height(10.dp))
                            RepliAction("Forget learned messages", {
                                learned.delete(profile.id)
                                styleRevision++
                            }, filled = false)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        RepliCard {
            RepliLabel("Your style, not your entire history", 17, RepliStyle.ink, bold = true)
            Spacer(Modifier.height(8.dp))
            RepliLabel("Optional: teach Repli with a few of your own messages while you scroll a chat. Review and choose every example before saving.",
                14, RepliStyle.muted)
            Spacer(Modifier.height(14.dp))
            RepliAction("Learn from visible messages", {
                navController.navigate(Routes.Settings.LearnByScrolling)
            }, filled = false)
            Spacer(Modifier.height(10.dp))
            RepliLabel("On-device recognition · no screenshots saved · no automatic upload", 12,
                RepliStyle.muted)
        }
    }

    if (addDialog) AddChatDialog(context, repository,
        onSaved = { profiles = repository.profiles(); addDialog = false },
        onDismiss = { addDialog = false })
    removeProfile?.let { profile ->
        AlertDialog(onDismissRequest = { removeProfile = null },
            title = { Text("Remove ${profile.name}?") },
            text = { Text("This removes the saved chat and its writing examples from this device.") },
            confirmButton = { TextButton(onClick = {
                repository.remove(profile.id)
                profiles = repository.profiles()
                removeProfile = null
            }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removeProfile = null }) { Text("Keep") } })
    }
    toneProfile?.let { profile ->
        AlertDialog(onDismissRequest = { toneProfile = null },
            title = { Text("Tone for ${profile.name}") },
            text = { Column {
                VoiceStyle.entries.forEach { style ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = profile.style == style, onClick = {
                            repository.updateStyle(profile.id, style)
                            profiles = repository.profiles()
                            toneProfile = null
                        })
                        Text(style.displayName)
                    }
                }
            } },
            confirmButton = { TextButton(onClick = { toneProfile = null }) { Text("Cancel") } })
    }
}

@Composable
private fun AddChatDialog(context: Context, repository: ProfileRepository,
    onSaved: () -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var style by remember { mutableStateOf(VoiceStyle.CASUAL) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Save a chat") },
        text = { Column {
            Text("A name and a tone. That's all you need to start.")
            Spacer(Modifier.height(16.dp))
            RepliLabel("Chat name", 13, RepliStyle.muted)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = name, onValueChange = {
                if (it.length <= 80) name = it
                error = null
            }, placeholder = { Text("e.g. Alex") }, singleLine = true,
                isError = error != null, supportingText = error?.let { message -> { Text(message) } },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(20.dp))
            RepliLabel("How should replies feel?", 14, RepliStyle.ink, bold = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                VoiceStyle.entries.forEach { choice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = style == choice, onClick = { style = choice })
                        RepliLabel(choice.displayName, 13, RepliStyle.ink)
                    }
                }
            }
            RepliLabel("Only saved on this device. You can change the tone later.", 12,
                RepliStyle.muted)
        } },
        confirmButton = { TextButton(onClick = {
            val normalized = name.trim()
            error = when {
                normalized.isEmpty() -> "Enter a chat name"
                repository.findByName(normalized) != null -> "This chat is already saved. Change its tone from Chats."
                else -> null
            }
            if (error == null) {
                repository.add(normalized, "Saved chat", style)
                onSaved()
            }
        }) { Text("Save chat") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

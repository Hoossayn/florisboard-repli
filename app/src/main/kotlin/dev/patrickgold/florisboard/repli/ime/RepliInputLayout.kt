package dev.patrickgold.florisboard.repli.ime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.repli.capture.ConversationTurn
import dev.patrickgold.florisboard.repli.voice.VoicePhase
import dev.patrickgold.florisboard.repli.voice.VoiceRecordingState
import org.florisboard.lib.compose.stringRes

private val Paper = Color(0xFFF3F0FA)
private val Ink = Color(0xFF27243A)
private val Muted = Color(0xFF6E6A80)
private val Accent = Color(0xFF6654D1)
private val AccentSoft = Color(0xFFEEEAFE)
private val Line = Color(0xFFE4DEEE)
private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun RepliInputLayout(modifier: Modifier = Modifier) {
    val imeController = LocalImeController.current
    val orchestrator = imeController.repliReply
    if (orchestrator == null) {
        LaunchedEffect(Unit) {
            imeController.updateStateBlocking {
                state = state.copy(flags = state.flags.withImeUiMode(ImeUiMode.TEXT))
            }
        }
        return
    }
    val ui by orchestrator.uiState.collectAsState()
    LaunchedEffect(ui.active) {
        if (!ui.active) {
            imeController.updateStateBlocking {
                state = state.copy(flags = state.flags.withImeUiMode(ImeUiMode.TEXT))
            }
        }
    }
    val keyboardHeight = FlorisImeSizing.imeUiHeight()
    val expandedHeight = minOf(LocalConfiguration.current.screenHeightDp.dp * 0.58f, 520.dp)
    val panelHeight = if (ui.reviewing || ui.approval != null || ui.suggestions.isNotEmpty() || ui.guidanceOpen) {
        maxOf(keyboardHeight, expandedHeight)
    } else keyboardHeight
    Column(
        modifier = modifier.fillMaxWidth().height(panelHeight).background(Paper),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { orchestrator.clear() },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringRes(R.string.repli_panel__back), tint = Ink)
            }
            Text("Repli replies", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (ui.reviewing || ui.suggestions.isNotEmpty()) {
                TextButton(onClick = { orchestrator.clear() }) { Text("Clear", color = Muted, fontSize = 12.sp) }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(42.dp)
                .clickable {
                    if (ui.showChatPicker) orchestrator.closeChatPicker() else orchestrator.openChatPicker()
                },
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Line),
        ) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Chat tone", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(ui.selectedProfileName ?: "Default", color = Ink, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ExpandMore, contentDescription = "Choose chat tone", tint = Accent,
                    modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(7.dp))
        if (ui.busy) LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Accent, trackColor = AccentSoft,
        )
        if (ui.guidanceOpen) {
            GuidanceSection(
                modifier = Modifier.weight(1f),
                initial = ui.guidanceText,
                voice = ui.voice,
                voicePreview = ui.voicePreview,
                voiceBase = ui.voiceBase,
                voiceAccepted = ui.voiceAccepted,
                voiceAcceptedRev = ui.voiceAcceptedRev,
                voiceStatus = ui.voiceStatus,
                voiceShowSettings = ui.voiceShowSettings,
                onApply = orchestrator::applyGuidance,
                onCancel = orchestrator::cancelGuidance,
                onMic = orchestrator::startVoice,
                onPause = orchestrator::pauseVoice,
                onResume = orchestrator::resumeVoice,
                onStop = orchestrator::stopVoice,
                onDiscard = orchestrator::discardVoiceSegment,
                onConsumeAccepted = orchestrator::consumeVoiceAccepted,
                onVoiceSettings = orchestrator::openVoiceSettings,
            )
            return@Column
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.showChatPicker) {
                TonePicker(
                    options = ui.chatOptions,
                    selectedName = ui.selectedProfileName,
                    onSelect = orchestrator::selectChat,
                )
            } else {
                ui.confirm?.let { confirm ->
                    Surface(shape = CardShape, color = AccentSoft) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(confirm.description, color = Ink, fontSize = 12.sp,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = orchestrator::confirmSuggestedSender) {
                                Text(confirm.label, color = Accent, fontSize = 12.sp)
                            }
                        }
                    }
                }
                when {
                    ui.reviewing -> ReviewBody(
                        turns = ui.reviewTurns,
                        frames = ui.reviewFrames,
                        canAddPage = ui.reviewFrames < 4,
                        instructions = ui.instructions,
                        onFlip = orchestrator::flipReviewSpeaker,
                        onRemove = orchestrator::removeReviewTurn,
                        onFullScreen = orchestrator::openFullScreenReview,
                        onDirection = orchestrator::openGuidance,
                        onAddPage = { orchestrator.beginCapture(append = true) },
                    )
                    ui.approval != null -> ApprovalBody(
                        approval = ui.approval!!,
                        turns = ui.contextTurns,
                        onReview = orchestrator::openReview,
                        onFullScreen = orchestrator::openFullScreenReview,
                        onDevice = orchestrator::dismissApproval,
                    )
                    ui.suggestions.isNotEmpty() -> {
                        Text("Reply ideas", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (ui.status.isNotBlank()) {
                            Text(ui.status, color = Muted, fontSize = 11.sp)
                        }
                        ui.suggestions.forEach { suggestion ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { orchestrator.insertSuggestion(suggestion) },
                                shape = CardShape,
                                color = Color.White,
                                border = BorderStroke(1.dp, Line),
                            ) {
                                Text(suggestion, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = Ink, fontSize = 14.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (ui.canGenerateMore) SmallAction("More replies", orchestrator::requestMoreSuggestions)
                            SmallAction("Review", orchestrator::openReview)
                            SmallAction("Direction", orchestrator::openGuidance)
                        }
                    }
                    else -> {
                        Text(ui.status.ifBlank { "Capture a chat to find your next reply." },
                            color = Muted, fontSize = 13.sp)
                        if (!ui.busy) {
                            SmallAction("Add reply direction", orchestrator::openGuidance)
                            Text("For longer chats, start on the oldest page and capture each newer view.",
                                color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        when {
            ui.showChatPicker -> PanelFooter("Done", onClick = orchestrator::closeChatPicker)
            ui.reviewing -> PanelFooter(
                if (ui.awaitingReview) "Generate replies" else "Use this context",
                enabled = ui.reviewTurns.isNotEmpty(),
                onClick = orchestrator::useReviewedContext,
            )
            ui.approval != null -> PanelFooter("Generate cloud replies", onClick = orchestrator::approveGenerate)
            !ui.busy && ui.suggestions.isEmpty() -> PanelFooter("Capture chat", onClick = orchestrator::beginSuggestion)
        }
    }
}

@Composable
private fun TonePicker(options: List<ChatOption>, selectedName: String?, onSelect: (String?) -> Unit) {
    Text("Choose a chat tone", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    ToneOption("Default tone", "No saved writing style", selectedName == null) { onSelect(null) }
    options.forEach { option ->
        ToneOption(option.name, option.styleName, option.name == selectedName) { onSelect(option.id) }
    }
    if (options.isEmpty()) {
        Text("Save a chat in Repli Chats to use its writing style here.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ToneOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = CardShape,
        color = if (selected) AccentSoft else Color.White,
        border = BorderStroke(1.dp, if (selected) Accent else Line),
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
            Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReviewBody(
    turns: List<ConversationTurn>,
    frames: Int,
    canAddPage: Boolean,
    instructions: String?,
    onFlip: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onFullScreen: () -> Unit,
    onDirection: () -> Unit,
    onAddPage: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Review context", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        SmallAction("Full screen", onFullScreen)
        Text("${turns.size} messages", color = Accent, fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallAction("Add direction", onDirection)
        if (canAddPage) SmallAction("Add page", onAddPage)
    }
    if (instructions != null) {
        Surface(shape = CardShape, color = AccentSoft) {
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                Text("Reply direction", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(instructions, color = Ink, fontSize = 12.sp)
            }
        }
    }
    Text(if (frames > 0) "Recent chat · $frames captured view${if (frames == 1) "" else "s"}"
        else "Recent chat", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    turns.forEachIndexed { index, turn ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = if (turn.fromMe) AccentSoft else Color.White,
            border = BorderStroke(1.dp, Line),
        ) {
            Row(Modifier.padding(start = 11.dp, end = 3.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (turn.fromMe) "You · change" else "Them · change",
                        color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onFlip(index) }.padding(bottom = 3.dp),
                    )
                    Text(turn.text, color = Ink, fontSize = 13.sp, lineHeight = 17.sp)
                }
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove message ${index + 1}",
                        tint = Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ApprovalBody(
    approval: RepliApprovalCard,
    turns: List<ConversationTurn>,
    onReview: () -> Unit,
    onFullScreen: () -> Unit,
    onDevice: () -> Unit,
) {
    Text("Review before cloud generation", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text("${approval.turnCount} messages · ${approval.stylePreset} tone · ${approval.exampleCount} examples",
        color = Muted, fontSize = 12.sp)
    approval.instructions?.let {
        Text("Direction: $it", color = Ink, fontSize = 12.sp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallAction("Review context", onReview)
        SmallAction("Full screen", onFullScreen)
        SmallAction("On-device", onDevice)
    }
    turns.takeLast(3).forEach { turn ->
        Surface(shape = CardShape, color = if (turn.fromMe) AccentSoft else Color.White,
            border = BorderStroke(1.dp, Line)) {
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                Text(if (turn.fromMe) "You" else "Them", color = Muted, fontSize = 11.sp)
                Text(turn.text, color = Ink, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PanelFooter(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp).height(45.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(32.dp).clickable(onClick = onClick).padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GuidanceSection(
    modifier: Modifier = Modifier,
    initial: String,
    voice: VoiceRecordingState?,
    voicePreview: String,
    voiceBase: String,
    voiceAccepted: String?,
    voiceAcceptedRev: Int,
    voiceStatus: String?,
    voiceShowSettings: Boolean,
    onApply: (String) -> Unit,
    onCancel: () -> Unit,
    onMic: (String) -> Unit,
    onPause: () -> Unit,
    onResume: (String) -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    onConsumeAccepted: () -> Unit,
    onVoiceSettings: () -> Unit,
) {
    var field by remember { mutableStateOf(initial) }
    var appliedRev by remember { mutableStateOf(0) }
    LaunchedEffect(initial) { field = initial; appliedRev = 0 }
    LaunchedEffect(voiceAcceptedRev) {
        if (voiceAcceptedRev > appliedRev && voiceAccepted != null) {
            appliedRev = voiceAcceptedRev
            field = voiceAccepted
            onConsumeAccepted()
        }
    }
    val voiceBusy = voice?.phase in setOf(VoicePhase.PREPARING, VoicePhase.LISTENING,
        VoicePhase.PAUSING, VoicePhase.PROCESSING)
    LaunchedEffect(voicePreview, voiceBusy) {
        if (voiceBusy && voicePreview.isNotBlank()) field = voicePreview
    }
    LaunchedEffect(voiceBusy) {
        if (!voiceBusy && appliedRev == voiceAcceptedRev && voice != null &&
            voice.phase !in setOf(VoicePhase.REVIEW, VoicePhase.PAUSED)) field = voiceBase
    }
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            Text("Guide this reply", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Add a little context or say how you want to respond before generating.",
                color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = field,
                onValueChange = { if (!voiceBusy && it.length <= 500) field = it },
                placeholder = { Text("For example: decline politely", color = Muted) },
                supportingText = { Text("${field.length} / 500", color = Muted, fontSize = 10.sp) },
                minLines = 2,
                maxLines = 3,
                enabled = !voiceBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Ink, unfocusedTextColor = Ink,
                    focusedBorderColor = Accent, unfocusedBorderColor = Line,
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                ),
            )
            if (voiceStatus != null) Text(voiceStatus, color = Muted, fontSize = 11.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (voice?.phase) {
                    VoicePhase.LISTENING, VoicePhase.PAUSING -> {
                        SmallAction("Pause", onPause)
                        SmallAction("Stop", onStop)
                    }
                    VoicePhase.PAUSED -> {
                        SmallAction("Resume") { onResume(field) }
                        SmallAction("Stop", onStop)
                        SmallAction("Discard", onDiscard)
                    }
                    VoicePhase.PROCESSING, VoicePhase.PREPARING -> SmallAction("Stop", onStop)
                    else -> SmallAction("Speak direction") { onMic(field) }
                }
                if (voiceShowSettings) SmallAction("Voice settings", onVoiceSettings)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !voiceBusy,
                modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Line)) { Text("Cancel", color = Accent) }
            Button(onClick = { onApply(field) }, enabled = !voiceBusy,
                modifier = Modifier.weight(2f).height(44.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Use direction", color = Color.White)
            }
        }
    }
}

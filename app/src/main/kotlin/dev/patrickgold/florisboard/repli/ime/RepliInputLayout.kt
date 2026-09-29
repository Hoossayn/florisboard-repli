package dev.patrickgold.florisboard.repli.ime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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

private val Paper: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF17151E) else Color(0xFFF3F0FA)
private val Card: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF25212F) else Color.White
private val Ink: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF4F1FA) else Color(0xFF27243A)
private val Muted: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFB8B1C6) else Color(0xFF6E6A80)
private val Accent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFAB9BFF) else Color(0xFF6654D1)
private val OnAccent: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF21183E) else Color.White
private val AccentSoft: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF383050) else Color(0xFFEEEAFE)
private val Line: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF453E52) else Color(0xFFE4DEEE)
private val ErrorCard: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF4A292D) else Color(0xFFFFEDEC)
private val ErrorLine: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF8A5357) else Color(0xFFE8B8B5)
private val ErrorText: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFC9C5) else Color(0xFF8B2925)
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
    val moreHeight = minOf(LocalConfiguration.current.screenHeightDp.dp * 0.48f, 440.dp)
    val panelHeight = when {
        ui.reviewing -> keyboardHeight + 20.dp
        ui.suggestions.isNotEmpty() && !ui.guidanceOpen -> maxOf(keyboardHeight, moreHeight)
        else -> keyboardHeight
    }
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
            Text(if (ui.reviewing) "Review context" else "Repli replies", color = Ink,
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (ui.reviewing) {
                TextButton(onClick = orchestrator::openFullScreenReview) {
                    Text("Full screen", color = Accent, fontSize = 11.sp)
                }
            } else if (ui.suggestions.isNotEmpty()) {
                TextButton(onClick = { orchestrator.clear() }) { Text("Clear", color = Muted, fontSize = 12.sp) }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(42.dp)
                .clickable {
                    if (ui.showChatPicker) orchestrator.closeChatPicker() else orchestrator.openChatPicker()
                },
            shape = RoundedCornerShape(15.dp),
            color = Card,
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
                        sourceStatus = if (ui.generationError == null) ui.status else "",
                        onFlip = orchestrator::flipReviewSpeaker,
                        onRemove = orchestrator::removeReviewTurn,
                        onDirection = orchestrator::openGuidance,
                        onAddPage = { orchestrator.beginCapture(append = true) },
                    )
                    ui.approval != null -> ApprovalBody(
                        approval = ui.approval!!,
                        turns = ui.contextTurns,
                        onReview = orchestrator::openReview,
                        onFullScreen = orchestrator::openFullScreenReview,
                        status = ui.status,
                    )
                    ui.suggestions.isNotEmpty() -> {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("More replies", color = Ink, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            SmallAction("Back to keyboard", orchestrator::backToKeyboard)
                        }
                        if (ui.status.isNotBlank()) Text(ui.status, color = Muted, fontSize = 11.sp)
                        ui.suggestions.forEach { suggestion ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { orchestrator.insertSuggestion(suggestion) },
                                shape = CardShape,
                                color = Card,
                                border = BorderStroke(1.dp, Line),
                            ) {
                                Text(suggestion, modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                    color = Ink, fontSize = 13.sp)
                            }
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
        if (ui.reviewing && ui.generationError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = CardShape,
                color = ErrorCard,
                border = BorderStroke(1.dp, ErrorLine),
            ) {
                Text(
                    ui.generationError.orEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = ErrorText, fontSize = 12.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            ui.showChatPicker -> PanelFooter("Done", onClick = orchestrator::closeChatPicker)
            ui.reviewing -> PanelFooter(
                "Generate replies",
                enabled = ui.reviewTurns.isNotEmpty(),
                onClick = orchestrator::useReviewedContext,
            )
            ui.approval != null -> PanelFooter("Generate cloud replies", onClick = orchestrator::approveGenerate)
            ui.suggestions.isNotEmpty() -> MoreRepliesFooter(
                onDirection = orchestrator::openGuidance,
                onMore = orchestrator::requestMoreSuggestions,
            )
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
        color = if (selected) AccentSoft else Card,
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
    sourceStatus: String,
    onFlip: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDirection: () -> Unit,
    onAddPage: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${turns.size} messages · $frames captured view${if (frames == 1) "" else "s"}",
            color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        SmallAction("Add direction", onDirection)
        if (canAddPage) SmallAction("Add page", onAddPage)
    }
    if (sourceStatus.isNotBlank()) Text(sourceStatus, color = Muted, fontSize = 11.sp,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
    if (instructions != null) {
        Text("Direction: $instructions", color = Accent, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onDirection))
    }
    turns.forEachIndexed { index, turn ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            color = if (turn.fromMe) AccentSoft else Card,
            border = BorderStroke(1.dp, Line),
        ) {
            Row(Modifier.padding(start = 11.dp, end = 3.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (turn.fromMe) "You" else "Them",
                            color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { onFlip(index) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.SwapHoriz,
                                contentDescription = "Switch speaker for message ${index + 1}",
                                tint = Accent, modifier = Modifier.size(17.dp))
                        }
                    }
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
    status: String,
) {
    Text("Review before cloud generation", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text("${approval.turnCount} messages · ${approval.stylePreset} tone · ${approval.exampleCount} examples",
        color = Muted, fontSize = 12.sp)
    if (status.isNotBlank()) Text(status, color = Muted, fontSize = 11.sp)
    approval.instructions?.let {
        Text("Direction: $it", color = Ink, fontSize = 12.sp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SmallAction("Review context", onReview)
        SmallAction("Full screen", onFullScreen)
    }
    turns.takeLast(3).forEach { turn ->
        Surface(shape = CardShape, color = if (turn.fromMe) AccentSoft else Card,
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
private fun MoreRepliesFooter(onDirection: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.weight(1f).height(44.dp).clickable(onClick = onDirection),
            shape = CardShape, color = Card, border = BorderStroke(1.dp, Line)) {
            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                Text("Tell Repli what you want…", color = Muted, fontSize = 12.sp)
            }
        }
        Button(onClick = onMore, modifier = Modifier.height(38.dp),
            shape = CardShape, colors = ButtonDefaults.buttonColors(containerColor = Accent),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text("More suggestions", color = OnAccent, fontSize = 11.sp)
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
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
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
                    focusedContainerColor = Card, unfocusedContainerColor = Card,
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
                Text("Use direction", color = OnAccent)
            }
        }
    }
}

@Composable
fun RepliInlineToneBar(modifier: Modifier = Modifier) {
    val controller = LocalImeController.current.repliReply ?: return
    val ui by controller.uiState.collectAsState()
    Surface(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)
        .height(42.dp),
        color = Card, shape = CardShape, border = BorderStroke(1.dp, Line)) {
        Row(Modifier.padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).clickable { controller.openChatPicker() },
                verticalAlignment = Alignment.CenterVertically) {
                Text("Chat tone", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(" · ${ui.selectedProfileName ?: "Default"}", color = Muted, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = controller::openGuidance,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                Text("Guide", color = Accent, fontSize = 11.sp)
            }
            TextButton(onClick = controller::clear,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                Text("Clear", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RepliInlineSuggestionRow(modifier: Modifier = Modifier) {
    val controller = LocalImeController.current.repliReply ?: return
    val ui by controller.uiState.collectAsState()
    val visibleReplies = ui.suggestions.takeLast(3)
    if (visibleReplies.isEmpty()) return
    val replyWidth = minOf(LocalConfiguration.current.screenWidthDp.dp * 0.72f, 300.dp)
    LazyRow(
        modifier = modifier.fillMaxWidth().height(54.dp).background(Paper),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(visibleReplies) { suggestion ->
            Surface(modifier = Modifier.width(replyWidth).height(44.dp)
                .clickable { controller.insertSuggestion(suggestion) },
                color = Card, shape = CardShape, border = BorderStroke(1.dp, Line)) {
                Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    Text(suggestion, color = Ink, fontSize = 12.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            Button(onClick = controller::openMoreReplies, modifier = Modifier.height(38.dp),
                shape = CardShape, colors = ButtonDefaults.buttonColors(containerColor = Accent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text("More suggestions", color = OnAccent, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun RepliInlineGuidance(modifier: Modifier = Modifier) {
    val controller = LocalImeController.current.repliReply ?: return
    val ui by controller.uiState.collectAsState()
    Surface(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
        color = Card, shape = CardShape, border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Guide the next replies", color = Ink, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = controller::cancelGuidance, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close direction", tint = Muted)
                }
            }
            Text("Say what you want changed, or generate another set", color = Muted, fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(62.dp).background(Paper, RoundedCornerShape(10.dp))
                .padding(10.dp), contentAlignment = Alignment.TopStart) {
                if (ui.guidanceText.isEmpty()) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("▍", color = Accent, fontSize = 13.sp)
                        Text("e.g. Shorter, warmer, or suggest next week", color = Muted,
                            fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Text(ui.guidanceText + "▍", color = Ink,
                        fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallAction("Clear", controller::clearGuidanceText)
                Text("${ui.guidanceText.length}/500", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                if (ui.voiceStatus != null) Text(ui.voiceStatus.orEmpty(), color = Muted,
                    fontSize = 10.sp, maxLines = 1)
                IconButton(onClick = { controller.startVoice(ui.guidanceText) },
                    modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Record reply direction",
                        tint = Accent, modifier = Modifier.size(21.dp))
                }
                Button(onClick = controller::applyInlineGuidance,
                    shape = CardShape, colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(if (ui.suggestions.isNotEmpty()) "Generate more" else "Use direction",
                        color = OnAccent, fontSize = 12.sp)
                }
            }
        }
    }
}

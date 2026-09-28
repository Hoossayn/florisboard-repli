package dev.patrickgold.florisboard.repli.ime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.repli.capture.ConversationTurn
import dev.patrickgold.florisboard.repli.voice.VoicePhase
import dev.patrickgold.florisboard.repli.voice.VoiceRecordingState
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

private val FallbackAccent = Color(0xFF6654D1)

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
    val buttonStyle = rememberSnyggThemeQuery(FlorisImeUi.RepliPanelButton.elementName)
    val buttonColor = buttonStyle.background().takeUnless { it.isUnspecified } ?: FallbackAccent

    SnyggBox(
        elementName = FlorisImeUi.RepliPanel.elementName,
        modifier = modifier.fillMaxSize(),
    ) {
        SnyggColumn(
            elementName = FlorisImeUi.RepliPanel.elementName,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SnyggIconButton(
                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                    onClick = {
                        orchestrator.clear()
                        imeController.updateStateBlocking {
                            state = state.copy(flags = state.flags.withImeUiMode(ImeUiMode.TEXT))
                        }
                    },
                ) {
                    SnyggIcon(imageVector = Icons.AutoMirrored.Default.ArrowBack)
                }
                SnyggText(
                    elementName = FlorisImeUi.RepliPanel.elementName,
                    text = stringRes(R.string.repli_panel__title),
                )
            }
            if (ui.status.isNotBlank()) {
                SnyggText(
                    elementName = FlorisImeUi.RepliPanel.elementName,
                    text = ui.status,
                )
            }
            if (ui.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = buttonColor)
            }
            ui.confirm?.let { confirm ->
                SnyggBox(elementName = FlorisImeUi.RepliPanelCard.elementName) {
                    Column(Modifier.padding(12.dp)) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = confirm.description,
                        )
                        Spacer(Modifier.height(8.dp))
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelButton.elementName,
                            onClick = { orchestrator.confirmSuggestedSender() },
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelButton.elementName,
                                text = confirm.label,
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SnyggText(
                    elementName = FlorisImeUi.RepliPanel.elementName,
                    modifier = Modifier.weight(1f),
                    text = ui.selectedProfileName?.let {
                        stringRes(R.string.repli_panel__for_chat, "name" to it)
                    } ?: stringRes(R.string.repli_panel__choose_chat),
                )
                if (ui.chatOptions.isNotEmpty()) {
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = {
                            if (ui.showChatPicker) orchestrator.closeChatPicker() else orchestrator.openChatPicker()
                        },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_panel__choose),
                        )
                    }
                }
            }
            if (ui.showChatPicker) {
                SnyggBox(elementName = FlorisImeUi.RepliPanelCard.elementName) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ui.chatOptions.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SnyggButton(
                                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                                    onClick = { orchestrator.selectChat(option.id) },
                                ) {
                                    SnyggText(
                                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                                        text = option.name,
                                    )
                                }
                                SnyggText(
                                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                                    text = option.styleName,
                                )
                            }
                        }
                    }
                }
            }
            if (ui.suggestions.isNotEmpty()) {
                ui.suggestions.forEach { suggestion ->
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = { orchestrator.insertSuggestion(suggestion) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = suggestion,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ui.canGenerateMore) {
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = { orchestrator.requestMoreSuggestions() },
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_panel__more),
                            )
                        }
                    }
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = { orchestrator.openReview() },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_panel__review_context),
                        )
                    }
                }
            }
            if (!ui.busy && ui.suggestions.isEmpty() && ui.approval == null && !ui.reviewing && !ui.guidanceOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelButton.elementName,
                        onClick = { orchestrator.beginSuggestion() },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelButton.elementName,
                            text = stringRes(R.string.repli_panel__suggest),
                        )
                    }
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = { orchestrator.openGuidance() },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_panel__direction),
                        )
                    }
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = { orchestrator.clear() },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_panel__clear),
                        )
                    }
                }
            }
            if (ui.reviewing) {
                ReviewSection(
                    turns = ui.reviewTurns,
                    frames = ui.reviewFrames,
                    onFlip = { orchestrator.flipReviewSpeaker(it) },
                    onRemove = { orchestrator.removeReviewTurn(it) },
                    onUse = { orchestrator.useReviewedContext() },
                    onAddView = { orchestrator.beginCapture(append = true) },
                    onClose = { orchestrator.closeReview() },
                )
            }
            if (ui.guidanceOpen) {
                GuidanceSection(
                    initial = ui.guidanceText,
                    voice = ui.voice,
                    voicePreview = ui.voicePreview,
                    voiceBase = ui.voiceBase,
                    voiceAccepted = ui.voiceAccepted,
                    voiceAcceptedRev = ui.voiceAcceptedRev,
                    voiceStatus = ui.voiceStatus,
                    voiceShowSettings = ui.voiceShowSettings,
                    onApply = { orchestrator.applyGuidance(it) },
                    onCancel = { orchestrator.cancelGuidance() },
                    onMic = { orchestrator.startVoice(it) },
                    onPause = { orchestrator.pauseVoice() },
                    onResume = { orchestrator.resumeVoice(it) },
                    onStop = { orchestrator.stopVoice() },
                    onDiscard = { orchestrator.discardVoiceSegment() },
                    onConsumeAccepted = { orchestrator.consumeVoiceAccepted() },
                    onVoiceSettings = { orchestrator.openVoiceSettings() },
                )
            }
            ui.approval?.let { approval ->
                SnyggBox(elementName = FlorisImeUi.RepliPanelCard.elementName) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_panel__approval_title),
                        )
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(
                                R.string.repli_panel__approval_summary,
                                "count" to approval.turnCount.toString(),
                                "style" to approval.stylePreset,
                                "examples" to approval.exampleCount.toString(),
                            ),
                        )
                        approval.instructions?.let {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = it,
                            )
                        }
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelButton.elementName,
                            onClick = { orchestrator.approveGenerate() },
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelButton.elementName,
                                text = stringRes(R.string.repli_panel__generate),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SnyggButton(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                onClick = { orchestrator.dismissApproval() },
                            ) {
                                SnyggText(
                                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                                    text = stringRes(R.string.repli_panel__use_on_device),
                                )
                            }
                            SnyggButton(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                onClick = { orchestrator.openReview() },
                            ) {
                                SnyggText(
                                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                                    text = stringRes(R.string.repli_panel__review_context),
                                )
                            }
                        }
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = { orchestrator.openFullScreenReview() },
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_panel__full_screen),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(
    turns: List<ConversationTurn>,
    frames: Int,
    onFlip: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onUse: () -> Unit,
    onAddView: () -> Unit,
    onClose: () -> Unit,
) {
    SnyggBox(elementName = FlorisImeUi.RepliPanelCard.elementName) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SnyggText(
                elementName = FlorisImeUi.RepliPanelCard.elementName,
                text = stringRes(R.string.repli_panel__review_title, "frames" to frames.toString()),
            )
            turns.forEachIndexed { index, turn ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = { onFlip(index) },
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = if (turn.fromMe) stringRes(R.string.repli_panel__me) else stringRes(R.string.repli_panel__them),
                        )
                    }
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = turn.text,
                        modifier = Modifier.weight(1f),
                    )
                    SnyggIconButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = { onRemove(index) },
                    ) {
                        SnyggIcon(imageVector = Icons.Default.Close)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnyggButton(
                    elementName = FlorisImeUi.RepliPanelButton.elementName,
                    onClick = onUse,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelButton.elementName,
                        text = stringRes(R.string.repli_panel__use_context),
                    )
                }
                SnyggButton(
                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                    onClick = onAddView,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = stringRes(R.string.repli_panel__add_view),
                    )
                }
                SnyggButton(
                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                    onClick = onClose,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = stringRes(R.string.repli_panel__close),
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidanceSection(
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
    LaunchedEffect(initial) {
        field = initial
        appliedRev = 0
    }
    LaunchedEffect(voiceAcceptedRev) {
        if (voiceAcceptedRev > appliedRev && voiceAccepted != null) {
            appliedRev = voiceAcceptedRev
            field = voiceAccepted
            onConsumeAccepted()
        }
    }
    val voiceBusy = voice?.phase == VoicePhase.PREPARING ||
        voice?.phase == VoicePhase.LISTENING ||
        voice?.phase == VoicePhase.PAUSING ||
        voice?.phase == VoicePhase.PROCESSING
    LaunchedEffect(voicePreview, voiceBusy) {
        if (voiceBusy && voicePreview.isNotBlank()) field = voicePreview
    }
    LaunchedEffect(voiceBusy) {
        if (!voiceBusy && appliedRev == voiceAcceptedRev) {
            // Provisional preview expired with nothing accepted: restore the draft.
            if (voice != null && voice.phase != VoicePhase.REVIEW && voice.phase != VoicePhase.PAUSED) {
                field = voiceBase
            }
        }
    }
    val cardStyle = rememberSnyggThemeQuery(FlorisImeUi.RepliPanelCard.elementName)
    val buttonStyle = rememberSnyggThemeQuery(FlorisImeUi.RepliPanelButton.elementName)
    val fieldTextColor = cardStyle.foreground().takeUnless { it.isUnspecified } ?: Color.Unspecified
    val accentColor = buttonStyle.background().takeUnless { it.isUnspecified } ?: FallbackAccent
    SnyggBox(elementName = FlorisImeUi.RepliPanelCard.elementName) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SnyggText(
                elementName = FlorisImeUi.RepliPanelCard.elementName,
                text = stringRes(R.string.repli_panel__direction_title),
            )
            OutlinedTextField(
                value = field,
                onValueChange = { if (!voiceBusy && it.length <= 500) field = it },
                label = {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = stringRes(R.string.repli_panel__direction_hint),
                    )
                },
                supportingText = {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = "${field.length} / 500",
                    )
                },
                maxLines = 2,
                enabled = !voiceBusy,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = fieldTextColor,
                    unfocusedTextColor = fieldTextColor,
                    disabledTextColor = fieldTextColor,
                    cursorColor = accentColor,
                    focusedBorderColor = accentColor,
                ),
            )
            if (voiceStatus != null) {
                SnyggText(
                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                    text = voiceStatus,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnyggButton(
                    elementName = FlorisImeUi.RepliPanelButton.elementName,
                    onClick = { onApply(field) },
                    enabled = !voiceBusy,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelButton.elementName,
                        text = stringRes(R.string.repli_panel__apply),
                    )
                }
                SnyggButton(
                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                    onClick = { field = "" },
                    enabled = !voiceBusy,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = stringRes(R.string.repli_panel__clear_field),
                    )
                }
                SnyggButton(
                    elementName = FlorisImeUi.RepliPanelCard.elementName,
                    onClick = onCancel,
                    enabled = !voiceBusy,
                ) {
                    SnyggText(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        text = stringRes(R.string.repli_panel__close),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (voice?.phase) {
                    VoicePhase.LISTENING, VoicePhase.PAUSING -> {
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = onPause,
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__pause),
                            )
                        }
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = onStop,
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__stop),
                            )
                        }
                    }
                    VoicePhase.PAUSED -> {
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = { onResume(field) },
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__resume),
                            )
                        }
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = onStop,
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__stop),
                            )
                        }
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = onDiscard,
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__discard),
                            )
                        }
                    }
                    VoicePhase.PROCESSING, VoicePhase.PREPARING -> {
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = onStop,
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__stop),
                            )
                        }
                    }
                    else -> {
                        SnyggButton(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            onClick = { onMic(field) },
                        ) {
                            SnyggText(
                                elementName = FlorisImeUi.RepliPanelCard.elementName,
                                text = stringRes(R.string.repli_voice__mic),
                            )
                        }
                    }
                }
                if (voiceShowSettings) {
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = onVoiceSettings,
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_voice__settings),
                        )
                    }
                }
                if (voice != null && !voiceBusy) {
                    SnyggButton(
                        elementName = FlorisImeUi.RepliPanelCard.elementName,
                        onClick = onDiscard,
                    ) {
                        SnyggText(
                            elementName = FlorisImeUi.RepliPanelCard.elementName,
                            text = stringRes(R.string.repli_voice__discard),
                        )
                    }
                }
            }
        }
    }
}

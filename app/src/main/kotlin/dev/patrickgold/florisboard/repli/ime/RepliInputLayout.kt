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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard3.LocalImeController
import dev.patrickgold.florisboard.repli.capture.ConversationTurn
import org.florisboard.lib.compose.stringRes

private val RepliPaper = Color(0xFFFAF8F5)
private val RepliInk = Color(0xFF27243A)
private val RepliAccent = Color(0xFF6654D1)

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

    Surface(modifier = modifier.fillMaxSize(), color = RepliPaper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    orchestrator.clear()
                    imeController.updateStateBlocking {
                        state = state.copy(flags = state.flags.withImeUiMode(ImeUiMode.TEXT))
                    }
                }) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = stringRes(R.string.repli_panel__back))
                }
                Text(stringRes(R.string.repli_panel__title), color = RepliInk)
            }
            if (ui.status.isNotBlank()) {
                Text(ui.status, color = RepliInk)
            }
            if (ui.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = RepliAccent)
            }
            ui.confirm?.let { confirm ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(confirm.description, color = RepliInk)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { orchestrator.confirmSuggestedSender() }) {
                            Text(confirm.label)
                        }
                    }
                }
            }
            if (ui.suggestions.isNotEmpty()) {
                ui.suggestions.forEach { suggestion ->
                    OutlinedButton(
                        onClick = { orchestrator.insertSuggestion(suggestion) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(suggestion, color = RepliInk)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ui.canGenerateMore) {
                        OutlinedButton(onClick = { orchestrator.requestMoreSuggestions() }) {
                            Text(stringRes(R.string.repli_panel__more))
                        }
                    }
                    OutlinedButton(onClick = { orchestrator.openReview() }) {
                        Text(stringRes(R.string.repli_panel__review_context))
                    }
                }
            }
            if (!ui.busy && ui.suggestions.isEmpty() && ui.approval == null && !ui.reviewing && !ui.guidanceOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { orchestrator.beginSuggestion() }) {
                        Text(stringRes(R.string.repli_panel__suggest))
                    }
                    OutlinedButton(onClick = { orchestrator.openGuidance() }) {
                        Text(stringRes(R.string.repli_panel__direction))
                    }
                    OutlinedButton(onClick = { orchestrator.clear() }) {
                        Text(stringRes(R.string.repli_panel__clear))
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
                    onApply = { orchestrator.applyGuidance(it) },
                    onCancel = { orchestrator.cancelGuidance() },
                )
            }
            ui.approval?.let { approval ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringRes(R.string.repli_panel__approval_title), color = RepliInk)
                        Text(
                            stringRes(
                                R.string.repli_panel__approval_summary,
                                "count" to approval.turnCount.toString(),
                                "style" to approval.stylePreset,
                                "examples" to approval.exampleCount.toString(),
                            ),
                            color = RepliInk,
                        )
                        approval.instructions?.let { Text(it, color = RepliInk) }
                        Button(onClick = { orchestrator.approveGenerate() }) {
                            Text(stringRes(R.string.repli_panel__generate))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { orchestrator.dismissApproval() }) {
                                Text(stringRes(R.string.repli_panel__use_on_device))
                            }
                            OutlinedButton(onClick = { orchestrator.openReview() }) {
                                Text(stringRes(R.string.repli_panel__review_context))
                            }
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
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringRes(R.string.repli_panel__review_title, "frames" to frames.toString()),
                color = RepliInk,
            )
            turns.forEachIndexed { index, turn ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onFlip(index) }) {
                        Text(if (turn.fromMe) stringRes(R.string.repli_panel__me) else stringRes(R.string.repli_panel__them))
                    }
                    Text(
                        turn.text,
                        color = RepliInk,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringRes(R.string.repli_panel__remove))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onUse) { Text(stringRes(R.string.repli_panel__use_context)) }
                OutlinedButton(onClick = onAddView) { Text(stringRes(R.string.repli_panel__add_view)) }
                OutlinedButton(onClick = onClose) { Text(stringRes(R.string.repli_panel__close)) }
            }
        }
    }
}

@Composable
private fun GuidanceSection(
    initial: String,
    onApply: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var field by remember { mutableStateOf(initial) }
    LaunchedEffect(initial) { field = initial }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringRes(R.string.repli_panel__direction_title), color = RepliInk)
            OutlinedTextField(
                value = field,
                onValueChange = { if (it.length <= 500) field = it },
                label = { Text(stringRes(R.string.repli_panel__direction_hint)) },
                supportingText = { Text("${field.length} / 500") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onApply(field) }) { Text(stringRes(R.string.repli_panel__apply)) }
                OutlinedButton(onClick = { field = "" }) { Text(stringRes(R.string.repli_panel__clear_field)) }
                OutlinedButton(onClick = onCancel) { Text(stringRes(R.string.repli_panel__close)) }
            }
        }
    }
}

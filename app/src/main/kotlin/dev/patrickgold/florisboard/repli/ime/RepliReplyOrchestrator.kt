package dev.patrickgold.florisboard.repli.ime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import dev.patrickgold.florisboard.ime.nlp.latin.TypingPredictionPolicy
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.repli.account.RepliAccountSessionRepository
import dev.patrickgold.florisboard.repli.account.RepliFirebaseAccountManager
import dev.patrickgold.florisboard.repli.capture.CaptureViewport
import dev.patrickgold.florisboard.repli.capture.ConversationTurn
import dev.patrickgold.florisboard.repli.capture.PendingVisionCaptureStore
import dev.patrickgold.florisboard.repli.capture.ReplyCaptureSession
import dev.patrickgold.florisboard.repli.capture.ReplyCaptureState
import dev.patrickgold.florisboard.repli.capture.ReplyConversation
import dev.patrickgold.florisboard.repli.capture.ReplyEditor
import dev.patrickgold.florisboard.repli.capture.ReplyPhase
import dev.patrickgold.florisboard.repli.capture.ReplyCaptureConsentActivity
import dev.patrickgold.florisboard.repli.capture.ReviewEvidenceStore
import dev.patrickgold.florisboard.repli.data.LearnedStyleRepository
import dev.patrickgold.florisboard.repli.data.ProfileRepository
import dev.patrickgold.florisboard.repli.data.RecentMessageRepository
import dev.patrickgold.florisboard.repli.data.RemoteGenerationPreferences
import dev.patrickgold.florisboard.repli.identity.ConfirmedConversationIdentity
import dev.patrickgold.florisboard.repli.identity.ConversationIdentityResolution
import dev.patrickgold.florisboard.repli.identity.ConversationIdentityResolver
import dev.patrickgold.florisboard.repli.identity.CapturedContactName
import dev.patrickgold.florisboard.repli.profile.ProfileMatcher
import dev.patrickgold.florisboard.repli.profile.RecentMessage
import dev.patrickgold.florisboard.repli.profile.VoiceProfile
import dev.patrickgold.florisboard.repli.profile.VoiceStyle
import dev.patrickgold.florisboard.repli.suggestions.OnDeviceReplyEngine
import dev.patrickgold.florisboard.repli.suggestions.PreparedRemoteReplyRequest
import dev.patrickgold.florisboard.repli.suggestions.RemoteReplyPrivacyPolicy
import dev.patrickgold.florisboard.repli.suggestions.RepliAccountSessionProvider
import dev.patrickgold.florisboard.repli.suggestions.ReplyGenerationCoordinator
import dev.patrickgold.florisboard.repli.suggestions.ServerMediatedContextEngine
import dev.patrickgold.florisboard.repli.suggestions.ServerMediatedReplyEngine
import dev.patrickgold.florisboard.repli.review.FullScreenContextReviewActivity
import dev.patrickgold.florisboard.repli.review.FullScreenContextReviewSession
import dev.patrickgold.florisboard.repli.voice.MicrophonePermissionActivity
import dev.patrickgold.florisboard.repli.voice.VoiceDeadline
import dev.patrickgold.florisboard.repli.voice.VoiceFailure
import dev.patrickgold.florisboard.repli.voice.VoiceGuidanceDependencies
import dev.patrickgold.florisboard.repli.voice.VoiceGuidanceRecorder
import dev.patrickgold.florisboard.repli.voice.VoiceGuidanceText
import dev.patrickgold.florisboard.repli.voice.VoicePhase
import dev.patrickgold.florisboard.repli.voice.VoiceRecordingState
import android.Manifest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class RepliConfirmChip(val label: String, val description: String)

data class RepliApprovalCard(
    val turnCount: Int,
    val instructions: String?,
    val stylePreset: String,
    val exampleCount: Int,
)

data class ChatOption(val id: String, val name: String, val styleName: String)

data class RepliReplyUiState(
    val active: Boolean = false,
    val status: String = "",
    val busy: Boolean = false,
    val confirm: RepliConfirmChip? = null,
    val suggestions: List<String> = emptyList(),
    val explanation: String? = null,
    val canGenerateMore: Boolean = false,
    val reviewing: Boolean = false,
    val reviewTurns: List<ConversationTurn> = emptyList(),
    val reviewFrames: Int = 0,
    val approval: RepliApprovalCard? = null,
    val guidanceOpen: Boolean = false,
    val guidanceText: String = "",
    val showChatPicker: Boolean = false,
    val chatOptions: List<ChatOption> = emptyList(),
    val selectedProfileName: String? = null,
    val voice: VoiceRecordingState? = null,
    val voicePreview: String = "",
    val voiceAccepted: String? = null,
    val voiceAcceptedRev: Int = 0,
    val voiceBase: String = "",
    val voiceStatus: String? = null,
    val voiceShowSettings: Boolean = false,
)

/**
 * FlorisBoard-native reply orchestrator. Owns the ported Repli engines and session,
 * exposes UI-agnostic [StateFlow] state, and never touches Views directly.
 * Cloud generation only runs from an explicitly approved [PreparedRemoteReplyRequest].
 */
class RepliReplyOrchestrator(
    private val appContext: Context,
    private val insertText: (String) -> Unit,
    private val hideKeyboard: () -> Unit,
    private val showKeyboard: () -> Unit,
    private val startActivity: (Intent) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(RepliReplyUiState())
    val uiState: StateFlow<RepliReplyUiState> = mutable.asStateFlow()

    private val coordinator = ReplyGenerationCoordinator(
        device = OnDeviceReplyEngine(),
        remote = BuildConfig.REPLI_BACKEND_URL
            .takeIf(ServerMediatedReplyEngine::isConfigured)
            ?.let { ServerMediatedReplyEngine(it, RepliAccountSessionProvider) },
    )
    private val contextEngine = BuildConfig.REPLI_BACKEND_URL
        .takeIf(ServerMediatedReplyEngine::isConfigured)
        ?.let { ServerMediatedContextEngine(it, RepliAccountSessionProvider) }

    private var generation: Job? = null
    private var captureLaunch: Job? = null
    private var editor: ReplyEditor? = null
    private var sensitive = true
    private var profiles: List<VoiceProfile> = emptyList()
    private var recentMessages: List<RecentMessage> = emptyList()
    private var resolution: ConversationIdentityResolution = ConversationIdentityResolution.None
    private var suggestion: ConversationIdentityResolution.Suggestion? = null
    private var selectedProfileId: String? = null
    private var confirmedIdentity: ConfirmedConversationIdentity? = null
    private var contextLoadGeneration = 0
    private var pendingRemoteRequest: PreparedRemoteReplyRequest? = null
    private var reviewingTurns: MutableList<ConversationTurn>? = null
    private var guidanceDraftId: String? = null
    private var guidanceOverride: String? = null
    private var fsOriginal: ReplyEditor? = null
    private var voiceRecorder: VoiceGuidanceRecorder? = null
    private var voiceBase: String = ""
    private var voiceDraftId: String? = null
    private var voiceReturnExpiry: Job? = null
    private var voiceAcceptedRev = 0
    private var voiceAccepted: String? = null
    private var voiceLastTranscript: String? = null
    private var voiceAvailable: Boolean? = null

    init {
        scope.launch {
            ReplyCaptureSession.state.collect { state ->
                onSessionState(state)
            }
        }
    }

    // Editor tracking: called synchronously from the IME input-start handler.
    // Returns true when this start is the deliberate capture return that must
    // restore the reply panel; every other start invalidates prior context.

    fun onStartInput(info: FlorisEditorInfo): Boolean {
        val incoming = ReplyEditor(info.packageName ?: "", info.base.fieldId, info.base.fieldName)
        val nowSensitive = !TypingPredictionPolicy.allows(info)
        val current = ReplyCaptureSession.state.value
        flogDebug { "RepliReply: onStartInput pkg=${incoming.packageName} field=${incoming.fieldId} sensitive=$nowSensitive session=${current?.id} phase=${current?.phase}" }
        if (current != null && fsOriginal != null && FullScreenContextReviewSession.isReturning(current.id)) {
            val original = fsOriginal
            if (original != null && FullScreenContextReviewSession.acceptsEditor(current.id, original, incoming, appContext.packageName)) {
                ReplyCaptureSession.rebindEditorForReviewReturn(current.id, incoming)
                FullScreenContextReviewSession.markRestored(current.id)
                editor = incoming
                sensitive = nowSensitive
                fsOriginal = null
                return true
            }
            FullScreenContextReviewSession.end(current.id)
            fsOriginal = null
        }
        if (current != null && current.busy && incoming == current.editor) {
            val consumed = ReplyCaptureSession.consumeKeyboardReturn(incoming)
            flogDebug { "RepliReply: round-trip restore consumed=$consumed id=${current.id}" }
            editor = incoming
            sensitive = nowSensitive
            return true
        }
        if (current != null) {
            flogDebug { "RepliReply: clearing session id=${current.id} phase=${current.phase}" }
            clear()
        }
        val changed = incoming != editor || nowSensitive != sensitive
        editor = incoming
        sensitive = nowSensitive
        selectedProfileId = null
        confirmedIdentity = null
        if (nowSensitive) {
            profiles = emptyList()
            recentMessages = emptyList()
            resolution = ConversationIdentityResolution.None
            suggestion = null
            publish()
        } else if (changed) {
            loadContext(incoming.packageName)
        }
        return false
    }

    private fun loadContext(packageName: String) {
        val generation = ++contextLoadGeneration
        scope.launch(Dispatchers.IO) {
            val loadedProfiles = ProfileRepository(appContext).profiles()
            val recent = RecentMessageRepository(appContext).recentFor(packageName)
            withContext(Dispatchers.Main) {
                if (generation != contextLoadGeneration) return@withContext
                profiles = loadedProfiles
                recentMessages = recent
                if (selectedProfileId != null && loadedProfiles.none { it.id == selectedProfileId }) {
                    selectedProfileId = null
                    confirmedIdentity = null
                }
                resolution = ConversationIdentityResolver.resolve(packageName, recent)
                suggestion = resolution as? ConversationIdentityResolution.Suggestion
                publish()
            }
        }
    }

    // Entry points

    fun beginSuggestion() {
        val target = editor ?: return
        if (sensitive) {
            update { it.copy(active = true, status = "Assistance is off for this field") }
            return
        }
        val state = ReplyCaptureSession.state.value
        if (state != null && state.busy) return
        flogDebug { "RepliReply: beginSuggestion turns=${state?.turns?.size} seed=${trustedSeed() != null}" }
        if (state != null && state.turns.isNotEmpty() && state.editor == target) {
            beginCapture(append = false)
            return
        }
        val seed = trustedSeed()
        if (seed != null) {
            update { it.copy(active = true) }
            ReplyCaptureSession.beginWithContext(target, listOf(ConversationTurn(seed, fromMe = false)))
        } else {
            beginCapture(append = false)
        }
    }

    fun confirmSuggestedSender() {
        val target = editor ?: return
        if (sensitive) return
        val current = suggestion ?: return
        if (ReplyCaptureSession.state.value?.busy == true) return
        // Stale-tap guard: re-resolve before trusting anything.
        val fresh = ConversationIdentityResolver.resolve(target.packageName, recentMessages)
        if (fresh != current) {
            resolution = fresh
            suggestion = fresh as? ConversationIdentityResolution.Suggestion
            publish()
            return
        }
        scope.launch(Dispatchers.IO) {
            val repository = ProfileRepository(appContext)
            val existing = repository.profiles().firstOrNull { ProfileMatcher.matches(current.message.sender, it) }
            val profile = existing ?: repository.add(current.message.sender, "Added from confirmed chat", VoiceStyle.CASUAL)
            val confirmed = ConversationIdentityResolver.confirm(current, profile)
            withContext(Dispatchers.Main) {
                if (confirmed == null) return@withContext
                profiles = repository.profiles()
                selectedProfileId = profile.id
                confirmedIdentity = confirmed
                update { it.copy(active = true) }
                ReplyCaptureSession.beginWithContext(target, listOf(ConversationTurn(current.message.text, fromMe = false)))
            }
        }
    }

    fun insertSuggestion(text: String) {
        flogDebug { "RepliReply: insertSuggestion len=${text.length}" }
        insertText(text)
    }

    fun requestMoreSuggestions() {
        val state = ReplyCaptureSession.state.value ?: return
        if (state.phase != ReplyPhase.READY || state.replies.isEmpty() || state.busy) return
        generate(state, more = true)
    }

    fun beginCapture(append: Boolean) {
        val target = editor ?: return
        if (sensitive) return
        generation?.cancel()
        captureLaunch?.cancel()
        closeVoiceRecorder()
        pendingRemoteRequest = null
        reviewingTurns = null
        guidanceDraftId = null
        guidanceOverride = null
        val metrics: DisplayMetrics = appContext.resources.displayMetrics
        // Full display size (not the app window): must match the capture service,
        // which sizes its virtual display from maximumWindowMetrics.
        val windowManager = appContext.getSystemService(WindowManager::class.java)
        val displayWidth: Int
        val displayHeight: Int
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            displayWidth = bounds.width()
            displayHeight = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val real = DisplayMetrics().also(windowManager.defaultDisplay::getRealMetrics)
            displayWidth = real.widthPixels
            displayHeight = real.heightPixels
        }
        val viewport = CaptureViewport(
            width = displayWidth,
            height = displayHeight,
            contentBottom = (displayHeight - (80 * metrics.density).toInt())
                .coerceAtLeast(displayHeight / 2),
            readyAt = 0L,
        )
        val begun = ReplyCaptureSession.begin(target, append, viewport)
        update { it.copy(active = true) }
        hideKeyboard()
        captureLaunch = scope.launch {
            delay(KEYBOARD_HIDE_SETTLE_MS)
            if (ReplyCaptureSession.state.value?.id != begun.id) return@launch
            try {
                startActivity(
                    Intent(appContext, ReplyCaptureConsentActivity::class.java)
                        .putExtra(ReplyCaptureConsentActivity.EXTRA_REQUEST_ID, begun.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) {
                ReplyCaptureSession.fail(begun.id, "Couldn't open screen-sharing permission. Try again from this chat.")
            }
        }
    }

    fun clear() {
        generation?.cancel()
        captureLaunch?.cancel()
        voiceReturnExpiry?.cancel()
        closeVoiceRecorder()
        pendingRemoteRequest = null
        reviewingTurns = null
        guidanceDraftId = null
        guidanceOverride = null
        fsOriginal = null
        ReplyCaptureSession.state.value?.id?.let { FullScreenContextReviewSession.end(it) }
        ReplyCaptureSession.clear()
        mutable.value = RepliReplyUiState()
    }

    // Manual chat picker

    fun openChatPicker() {
        if (profiles.isEmpty()) {
            update { it.copy(active = true, status = "No saved chats yet — add one in Repli chats") }
            publish()
            return
        }
        update { it.copy(active = true, showChatPicker = true) }
        publish()
    }

    fun closeChatPicker() {
        update { it.copy(showChatPicker = false) }
        publish()
    }

    fun selectChat(id: String) {
        val profile = profiles.firstOrNull { it.id == id } ?: return
        update { it.copy(showChatPicker = false) }
        if (id == selectedProfileId) {
            publish()
            return
        }
        val state = ReplyCaptureSession.state.value
        if (state != null && state.phase == ReplyPhase.DRAFT) {
            // Switching chats while directing discards the draft scope.
            clear()
        }
        selectedProfileId = id
        confirmedIdentity = null
        val current = ReplyCaptureSession.state.value
        if (current != null && current.turns.isNotEmpty() && !current.busy) {
            generate(current)
        }
        publish()
    }

    // Review

    fun openReview() {
        val state = ReplyCaptureSession.state.value ?: return
        if (state.turns.isEmpty() || state.busy) return
        if (state.phase == ReplyPhase.CAPTURE_REVIEW || state.phase == ReplyPhase.APPROVAL) return
        reviewingTurns = state.turns.toMutableList()
        publish()
    }

    fun closeReview() {
        reviewingTurns = null
        publish()
    }

    fun flipReviewSpeaker(index: Int) {
        val turns = reviewingTurns ?: return
        if (index !in turns.indices) return
        turns[index] = turns[index].copy(fromMe = !turns[index].fromMe)
        publish()
    }

    fun removeReviewTurn(index: Int) {
        val turns = reviewingTurns ?: return
        if (index !in turns.indices) return
        turns.removeAt(index)
        publish()
    }

    fun useReviewedContext() {
        val state = ReplyCaptureSession.state.value ?: return
        val edited = reviewingTurns ?: return
        reviewingTurns = null
        ReplyCaptureSession.update(state.id) {
            it.copy(turns = edited.toList(), phase = ReplyPhase.CONTEXT, replies = emptyList())
        }
    }

    // Guidance (text only; voice mic wiring follows with the IME voice work)

    fun openGuidance() {
        val target = editor ?: return
        if (sensitive) return
        if (ReplyCaptureSession.state.value?.busy == true) return
        generation?.cancel()
        pendingRemoteRequest = null
        closeVoiceRecorder()
        val draft = ReplyCaptureSession.editGuidance(target)
        guidanceDraftId = draft.id
        guidanceOverride = null
        update { it.copy(active = true) }
        publish()
    }

    fun applyGuidance(text: String) {
        val id = guidanceDraftId ?: return
        try {
            ReplyCaptureSession.finishGuidance(id, text)
        } catch (_: IllegalArgumentException) {
            update { it.copy(status = "Response guidance is too long (500 characters max)") }
            return
        }
        guidanceDraftId = null
        guidanceOverride = null
        closeVoiceRecorder()
        publish()
    }

    fun cancelGuidance() {
        val id = guidanceDraftId
        val state = ReplyCaptureSession.state.value
        if (id != null && state != null && state.id == id) {
            // Closing keeps the previously applied direction; typed text is discarded.
            ReplyCaptureSession.finishGuidance(id, state.instructions)
        }
        guidanceDraftId = null
        guidanceOverride = null
        closeVoiceRecorder()
        publish()
    }

    // Voice guidance (on-device transcription into the internal direction field)

    fun startVoice(fieldText: String) {
        val target = editor ?: return
        val state = ReplyCaptureSession.state.value ?: return
        if (state.phase != ReplyPhase.DRAFT || state.id != guidanceDraftId || sensitive || state.editor != target) return
        if (voiceRecorder?.busy == true) return
        closeVoiceRecorder()
        val base = fieldText.take(RemoteReplyPrivacyPolicy.MAX_INSTRUCTION_CHARACTERS)
        voiceBase = base
        voiceDraftId = state.id
        voiceAvailable = runCatching { VoiceGuidanceDependencies.factory(appContext).available() }.getOrDefault(false)
        val recorder = makeVoiceRecorder(state.id)
        voiceRecorder = recorder
        if (isMicGranted()) {
            recorder.start(true)
        } else {
            if (!ReplyCaptureSession.beginMicrophonePermission(state.id, base)) {
                update { it.copy(status = "Response guidance is too long (500 characters max)") }
                closeVoiceRecorder()
                return
            }
            guidanceDraftId = null
            guidanceOverride = null
            publish()
            voiceReturnExpiry?.cancel()
            voiceReturnExpiry = scope.launch {
                delay(MICROPHONE_PERMISSION_TIMEOUT_MS)
                val current = ReplyCaptureSession.state.value
                if (current?.id == state.id && current.phase == ReplyPhase.MICROPHONE_PERMISSION) clear()
            }
            try {
                startActivity(
                    Intent(appContext, MicrophonePermissionActivity::class.java)
                        .putExtra(MicrophonePermissionActivity.EXTRA_REQUEST_ID, state.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: Exception) {
                ReplyCaptureSession.returnFromMicrophonePermission(state.id, false)
            }
        }
    }

    fun pauseVoice() {
        voiceRecorder?.pause()
    }

    fun resumeVoice(fieldText: String) {
        val id = voiceDraftId ?: return
        if (ReplyCaptureSession.state.value?.id != id) return
        voiceBase = fieldText.take(RemoteReplyPrivacyPolicy.MAX_INSTRUCTION_CHARACTERS)
        voiceRecorder?.start(true)
    }

    fun stopVoice() {
        voiceRecorder?.stop()
    }

    fun discardVoiceSegment() {
        // Drops only the provisional preview; accepted text stays in the field.
        voiceRecorder?.cancel()
    }

    fun consumeVoiceAccepted() {
        update { it.copy(voiceAccepted = null) }
    }

    fun openVoiceSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
            update { it.copy(status = appContext.getString(R.string.repli_voice__settings_unavailable)) }
        }
    }

    private fun makeVoiceRecorder(draftId: String): VoiceGuidanceRecorder {
        val factory = VoiceGuidanceDependencies.factory(appContext)
        return VoiceGuidanceRecorder(
            factory = factory,
            schedule = { ms, action ->
                val job = scope.launch {
                    delay(ms)
                    if (voiceDraftId == draftId) action()
                }
                VoiceDeadline { job.cancel() }
            },
            changed = { vs -> onVoiceChanged(draftId, vs) },
        )
    }

    private fun onVoiceChanged(draftId: String, vs: VoiceRecordingState) {
        if (voiceDraftId != draftId) return
        if (vs.phase == VoicePhase.REVIEW || vs.phase == VoicePhase.PAUSED) {
            val transcript = vs.transcript
            if (transcript != null && transcript != voiceLastTranscript) {
                voiceLastTranscript = transcript
                val accepted = VoiceGuidanceText.combine(voiceBase, transcript)
                if (accepted == null) {
                    update { it.copy(status = appContext.getString(R.string.repli_voice__too_long)) }
                } else {
                    voiceAcceptedRev += 1
                    voiceAccepted = accepted
                    voiceBase = accepted
                }
            }
        }
        publish()
    }

    private fun closeVoiceRecorder() {
        voiceRecorder?.close()
        voiceRecorder = null
        voiceDraftId = null
        voiceReturnExpiry?.cancel()
        voiceBase = ""
        voiceAccepted = null
        voiceLastTranscript = null
    }

    private fun isMicGranted(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // Approval

    fun approveGenerate() {
        val state = ReplyCaptureSession.state.value ?: return
        val displayed = pendingRemoteRequest ?: return
        if (state.phase != ReplyPhase.APPROVAL) return
        scope.launch(Dispatchers.IO) {
            val profile = selectedProfile()
            val snapshot = profile?.let { LearnedStyleRepository(appContext).get(it.id) }
            val current = ReplyCaptureSession.state.value ?: return@launch
            val fresh = RemoteReplyPrivacyPolicy.prepare(
                current.turns, profile?.style, snapshot, instructions = current.instructions,
            )
            if (fresh.context != displayed.context || fresh.instructions != displayed.instructions) return@launch
            withContext(Dispatchers.Main) {
                if (ReplyCaptureSession.state.value?.id != state.id) return@withContext
                generate(state, approved = displayed)
            }
        }
    }

    fun dismissApproval() {
        val state = ReplyCaptureSession.state.value ?: return
        if (state.phase != ReplyPhase.APPROVAL) return
        // Explicit on-device choice: never re-enter remote approval.
        generateNow(state, approved = null, more = false)
    }

    fun openFullScreenReview() {
        val target = editor ?: return
        val state = ReplyCaptureSession.state.value ?: return
        val request = pendingRemoteRequest ?: return
        if (state.phase != ReplyPhase.APPROVAL || state.editor != target) return
        fsOriginal = state.editor
        FullScreenContextReviewSession.begin(state.id)
        try {
            startActivity(FullScreenContextReviewActivity.intent(appContext, state.id, request))
        } catch (_: Exception) {
            FullScreenContextReviewSession.end(state.id)
            fsOriginal = null
            update { it.copy(status = "Couldn't open full-screen review") }
        }
    }

    // Session dispatch

    private fun onSessionState(state: ReplyCaptureState?) {
        flogDebug { "RepliReply: session id=${state?.id} phase=${state?.phase} turns=${state?.turns?.size} replies=${state?.replies?.size}" }
        if (state == null) {
            generation?.cancel()
            captureLaunch?.cancel()
            voiceReturnExpiry?.cancel()
            closeVoiceRecorder()
            pendingRemoteRequest = null
            reviewingTurns = null
            guidanceDraftId = null
            guidanceOverride = null
            mutable.value = RepliReplyUiState()
            return
        }
        if (guidanceDraftId != null && (state.id != guidanceDraftId || state.phase != ReplyPhase.DRAFT)) {
            guidanceDraftId = null
        }
        if (voiceDraftId != null && (state.id != voiceDraftId || state.phase != ReplyPhase.DRAFT)) {
            // Recording only survives inside its own DRAFT; permission round trips
            // are handled by MICROPHONE_RETURNING below.
            if (state.phase != ReplyPhase.MICROPHONE_PERMISSION &&
                state.phase != ReplyPhase.MICROPHONE_RETURNING
            ) {
                closeVoiceRecorder()
            }
        }
        when (state.phase) {
            ReplyPhase.RETURNING -> Unit
            ReplyPhase.MICROPHONE_RETURNING -> resumeAfterMicrophonePermission(state)
            ReplyPhase.CAPTURE_REVIEW -> handleCaptureReview(state)
            ReplyPhase.CONTEXT -> {
                reviewingTurns = null
                if (state.awaitingKeyboardReturn) showKeyboard()
                generate(state)
            }
            ReplyPhase.ERROR -> {
                if (state.awaitingKeyboardReturn) showKeyboard()
            }
            else -> Unit
        }
        publish()
    }

    private fun resumeAfterMicrophonePermission(state: ReplyCaptureState) {
        voiceReturnExpiry?.cancel()
        voiceReturnExpiry = scope.launch {
            delay(MICROPHONE_RETURN_SETTLE_MS)
            val current = ReplyCaptureSession.state.value ?: return@launch
            val target = editor ?: return@launch
            val resumed = ReplyCaptureSession.resumeMicrophonePermission(current.id, target)
            if (resumed == null) {
                clear()
                return@launch
            }
            guidanceDraftId = current.id
            guidanceOverride = resumed.draft
            voiceBase = resumed.draft
            if (resumed.startRecording && isMicGranted()) {
                closeVoiceRecorder()
                voiceDraftId = current.id
                val recorder = makeVoiceRecorder(current.id)
                voiceRecorder = recorder
                recorder.start(true)
            } else if (!isMicGranted()) {
                update { it.copy(active = true, status = appContext.getString(R.string.repli_voice__permission)) }
            }
            publish()
        }
    }

    private fun handleCaptureReview(state: ReplyCaptureState) {
        val pending = PendingVisionCaptureStore.peek(state.id)
        val canUseVision = pending != null && remoteEnabled() && contextEngine != null
        if (!canUseVision) {
            val taken = PendingVisionCaptureStore.take(state.id)
            taken?.eraseImages()
            val turns = ReplyConversation.mergeCapture(
                state.turns, taken?.localTurns.orEmpty(),
            )
            if (turns.isEmpty() && state.turns.isEmpty()) {
                ReplyCaptureSession.fail(state.id, "This looks like a new or empty chat. Send the first message before capturing context.")
            } else {
                ReplyCaptureSession.update(state.id) {
                    it.copy(phase = ReplyPhase.CONTEXT, viewport = null, turns = turns, message = "Finding replies on your phone…")
                }
            }
            return
        }
        scope.launch(Dispatchers.IO) {
            val taken = PendingVisionCaptureStore.take(state.id) ?: return@launch
            ReplyCaptureSession.update(state.id) {
                it.copy(phase = ReplyPhase.READING, viewport = null, message = "AI is reading this chat image…")
            }
            try {
                ReviewEvidenceStore.put(state.id, taken.images)
                val ai = withTimeout(AI_READING_TIMEOUT_MS) { contextEngine!!.extract(taken.images) }
                if (ReplyCaptureSession.state.value?.id != state.id) {
                    ReviewEvidenceStore.discard(state.id)
                    return@launch
                }
                ai.contactName?.let { selectCapturedContact(it) }
                val captured = ReplyConversation.mergeVisualBubbles(ai.bubbles.orEmpty(), taken.images.size)
                    .ifEmpty { ai.turns }
                val merged = ReplyConversation.reconcileAiWithLocal(captured, taken.localTurns)
                    .let { ReplyConversation.mergeCapture(taken.baseTurns, it) }
                withContext(Dispatchers.Main) {
                    if (merged.isEmpty()) {
                        ReviewEvidenceStore.discard(state.id)
                        ReplyCaptureSession.fail(state.id, "This looks like a new or empty chat. Send the first message before capturing context.")
                    } else {
                        ReplyCaptureSession.update(state.id) {
                            it.copy(phase = ReplyPhase.CONTEXT, turns = merged, message = "Finding replies from AI-read context…")
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                ReviewEvidenceStore.discard(state.id)
                throw cancellation
            } catch (_: Exception) {
                ReviewEvidenceStore.discard(state.id)
                val fallback = ReplyConversation.mergeCapture(taken.baseTurns, taken.localTurns)
                withContext(Dispatchers.Main) {
                    if (fallback.isEmpty()) {
                        ReplyCaptureSession.fail(state.id, "AI reading was unavailable and on-device reading found no messages. Try a clearer capture.")
                    } else {
                        ReplyCaptureSession.update(state.id) {
                            it.copy(phase = ReplyPhase.CONTEXT, turns = fallback, message = "AI reading unavailable · using on-device text")
                        }
                    }
                }
            } finally {
                taken.eraseImages()
            }
        }
    }

    private fun generate(state: ReplyCaptureState, approved: PreparedRemoteReplyRequest? = null, more: Boolean = false) {
        if (useRemote() && approved == null && !more) {
            prepareRemoteApproval(state)
            return
        }
        generateNow(state, approved, more)
    }

    private fun prepareRemoteApproval(state: ReplyCaptureState) {
        generation?.cancel()
        ReplyCaptureSession.update(state.id) {
            it.copy(phase = ReplyPhase.GENERATING, replies = emptyList(), message = "Preparing context review…")
        }
        generation = scope.launch(Dispatchers.IO) {
            try {
                val profile = selectedProfile()
                val snapshot = profile?.let { LearnedStyleRepository(appContext).get(it.id) }
                val prepared = RemoteReplyPrivacyPolicy.prepare(
                    state.turns, profile?.style, snapshot, instructions = state.instructions,
                )
                withContext(Dispatchers.Main) {
                    val current = ReplyCaptureSession.state.value
                    if (current?.id != state.id) return@withContext
                    pendingRemoteRequest = prepared
                    ReplyCaptureSession.update(state.id) {
                        it.copy(phase = ReplyPhase.APPROVAL, replies = emptyList(), message = "")
                    }
                    publish()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (ReplyCaptureSession.state.value?.id == state.id) generateNow(state, null, more = false)
                }
            }
        }
    }

    private fun generateNow(state: ReplyCaptureState, approved: PreparedRemoteReplyRequest?, more: Boolean) {
        generation?.cancel()
        ReviewEvidenceStore.discard(state.id)
        pendingRemoteRequest = null
        val keep = if (more) state.replies else emptyList()
        ReplyCaptureSession.update(state.id) {
            it.copy(
                phase = ReplyPhase.GENERATING,
                replies = keep,
                message = if (approved == null) "Finding replies on your phone…" else "Generating from approved context…",
            )
        }
        publish()
        generation = scope.launch(Dispatchers.IO) {
            try {
                val style = selectedProfile()?.let { LearnedStyleRepository(appContext).getStyle(it.id) }
                val result = withTimeout(GENERATION_TIMEOUT_MS) {
                    coordinator.suggest(state.turns, style, approved, state.instructions)
                }
                withContext(Dispatchers.Main) {
                    if (ReplyCaptureSession.state.value?.id != state.id) return@withContext
                    ReplyCaptureSession.update(state.id) {
                        it.copy(
                            phase = ReplyPhase.READY,
                            replies = (keep + result.replies).distinct().take(3),
                            message = result.explanation,
                        )
                    }
                    flogDebug { "RepliReply: READY replies=${result.replies.size} origin=${result.origin}" }
                    publish()
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    if (ReplyCaptureSession.state.value?.id != state.id) return@withContext
                    if (more) {
                        ReplyCaptureSession.update(state.id) { it.copy(phase = ReplyPhase.READY, message = "More replies timed out. Try again.") }
                    } else {
                        ReplyCaptureSession.fail(state.id, "Reply generation timed out. Review the context and try again.")
                    }
                    publish()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                flogError { "RepliReply: generateNow failed: ${error.message}" }
                withContext(Dispatchers.Main) {
                    if (ReplyCaptureSession.state.value?.id != state.id) return@withContext
                    if (more) {
                        ReplyCaptureSession.update(state.id) { it.copy(phase = ReplyPhase.READY, message = "Couldn't generate more replies. Try again.") }
                    } else {
                        ReplyCaptureSession.fail(state.id, "Neither reply engine could generate replies. Review the context and retry.")
                    }
                    publish()
                }
            }
        }
    }

    // Helpers

    private fun trustedSeed(): String? {
        val target = editor ?: return null
        return ConversationIdentityResolver.trustedIncomingText(
            confirmedIdentity, selectedProfileId, target.packageName, recentMessages,
        )
    }

    private fun selectedProfile(): VoiceProfile? =
        profiles.firstOrNull { it.id == selectedProfileId }

    private fun useRemote(): Boolean {
        val state = ReplyCaptureSession.state.value ?: return false
        if (state.turns.isEmpty()) return false
        if (coordinatorHasRemote().not()) return false
        if (!RemoteGenerationPreferences(appContext).enabled) return false
        return RepliAccountSessionRepository.bearerToken() != null ||
            RepliFirebaseAccountManager.state.value.signedIn
    }

    private fun coordinatorHasRemote(): Boolean =
        BuildConfig.REPLI_BACKEND_URL.takeIf(ServerMediatedReplyEngine::isConfigured) != null

    private fun remoteEnabled(): Boolean =
        RemoteGenerationPreferences(appContext).enabled &&
            (RepliAccountSessionRepository.bearerToken() != null ||
                RepliFirebaseAccountManager.state.value.signedIn)

    private fun selectCapturedContact(name: String) {
        val prepared = CapturedContactName.prepare(name) ?: return
        scope.launch(Dispatchers.IO) {
            val repository = ProfileRepository(appContext)
            val existing = repository.findByName(prepared)
            val profile = existing ?: repository.add(prepared, "Added from AI capture", VoiceStyle.CASUAL)
            withContext(Dispatchers.Main) {
                profiles = repository.profiles()
                selectedProfileId = profile.id
                confirmedIdentity = null
                resolution = ConversationIdentityResolution.None
                suggestion = null
                publish()
            }
        }
    }

    private fun update(transform: (RepliReplyUiState) -> RepliReplyUiState) {
        mutable.value = transform(mutable.value)
        publish()
    }

    private fun publish() {
        val session = ReplyCaptureSession.state.value
        val current = mutable.value
        val active = current.active || session != null
        val review = reviewingTurns
        val approval = pendingRemoteRequest?.takeIf { session?.phase == ReplyPhase.APPROVAL }?.let {
            RepliApprovalCard(
                turnCount = it.context.size,
                instructions = it.instructions,
                stylePreset = it.style.preset,
                exampleCount = it.style.examples.size,
            )
        }
        val confirm = suggestion?.takeIf { session == null || !session.busy }?.let {
            val candidate = it.message.sender.take(PROFILE_NAME_PREVIEW_LIMIT)
            val existing = profiles.firstOrNull { profile -> ProfileMatcher.matches(it.message.sender, profile) }
            if (existing == null) {
                RepliConfirmChip(
                    label = "Save $candidate?",
                    description = "Confirm this chat is with ${it.message.sender}, save its tone, and use its recent message",
                )
            } else {
                RepliConfirmChip(
                    label = "Use $candidate?",
                    description = "Confirm this chat is with ${it.message.sender} and use its recent message",
                )
            }
        }
        val draftId = guidanceDraftId
        val guidanceOpen = draftId != null && session?.id == draftId && session.phase == ReplyPhase.DRAFT
        val voiceState = voiceRecorder?.let { if (voiceDraftId != null) it.state else null }
        mutable.value = current.copy(
            active = active,
            busy = session?.busy == true,
            status = when {
                session == null && !current.active -> ""
                session == null -> current.status.ifBlank { defaultIdleStatus() }
                else -> session.message
            },
            confirm = confirm,
            suggestions = session?.replies.orEmpty(),
            explanation = null,
            canGenerateMore = session?.phase == ReplyPhase.READY && session.replies.isNotEmpty(),
            reviewing = review != null,
            reviewTurns = review.orEmpty(),
            reviewFrames = session?.frames ?: 0,
            approval = approval,
            guidanceOpen = guidanceOpen,
            guidanceText = if (guidanceOpen) guidanceOverride ?: session?.instructions.orEmpty() else "",
            showChatPicker = current.showChatPicker && profiles.isNotEmpty(),
            chatOptions = profiles.map { ChatOption(it.id, it.name, it.style.displayName) },
            selectedProfileName = selectedProfile()?.name,
            voice = voiceState,
            voicePreview = voiceState?.partial?.let { VoiceGuidanceText.combine(voiceBase, it) } ?: "",
            voiceAccepted = voiceAccepted,
            voiceAcceptedRev = voiceAcceptedRev,
            voiceBase = voiceBase,
            voiceStatus = voiceStatusFor(voiceState),
            voiceShowSettings = voiceState != null &&
                (voiceAvailable == false || voiceState.failure in
                    setOf(VoiceFailure.MODEL_MISSING, VoiceFailure.LANGUAGE, VoiceFailure.SERVICE, VoiceFailure.AUDIO)),
        )
    }

    private fun defaultIdleStatus(): String = when (val r = resolution) {        is ConversationIdentityResolution.Suggestion -> "Recent message · confirm before use"
        is ConversationIdentityResolution.Ambiguous -> "Several recent chats · use screen capture"
        else -> if (selectedProfile() != null) "Capture this chat to suggest replies"
        else "Suggest replies · saving a chat is optional"
    }

    private fun voiceStatusFor(state: VoiceRecordingState?): String? {
        if (state == null) return null
        val res = appContext.resources
        return when (state.phase) {
            VoicePhase.PREPARING -> res.getString(R.string.repli_voice__preparing)
            VoicePhase.LISTENING -> res.getString(R.string.repli_voice__listening)
            VoicePhase.PAUSING -> res.getString(R.string.repli_voice__pausing)
            VoicePhase.PAUSED -> res.getString(R.string.repli_voice__paused)
            VoicePhase.PROCESSING -> res.getString(R.string.repli_voice__processing)
            VoicePhase.ERROR -> when (state.failure) {
                VoiceFailure.NO_SPEECH -> res.getString(R.string.repli_voice__no_speech)
                VoiceFailure.TOO_LONG -> res.getString(R.string.repli_voice__too_long)
                VoiceFailure.UNAVAILABLE -> res.getString(R.string.repli_voice__unavailable)
                VoiceFailure.PERMISSION -> res.getString(R.string.repli_voice__permission)
                VoiceFailure.TIMEOUT -> res.getString(R.string.repli_voice__timeout)
                else -> res.getString(R.string.repli_voice__failed)
            }
            else -> null
        }
    }

    private companion object {
        const val KEYBOARD_HIDE_SETTLE_MS = 300L
        const val AI_READING_TIMEOUT_MS = 50_000L
        const val GENERATION_TIMEOUT_MS = 25_000L
        const val MICROPHONE_PERMISSION_TIMEOUT_MS = 30_000L
        const val MICROPHONE_RETURN_SETTLE_MS = 150L
        const val PROFILE_NAME_PREVIEW_LIMIT = 14
    }
}

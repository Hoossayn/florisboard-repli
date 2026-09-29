package dev.patrickgold.florisboard.repli.account

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import dev.patrickgold.florisboard.BuildConfig
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RepliAccountState(
    val configured: Boolean = false,
    val busy: Boolean = false,
    val signedIn: Boolean = false,
    val email: String? = null,
    val errorMessage: String? = null,
)

/**
 * Firebase owns the durable account credential. Repli backend sessions are
 * exchanged on demand and remain only in [RepliAccountSessionRepository] memory.
 * Network is used only for sign-in/session exchange/account deletion and,
 * later, for user-approved captured-text uploads to the first-party backend.
 */
object RepliFirebaseAccountManager {
    private val initLock = Any()
    private val operationMutex = Mutex()
    private val mutableState = MutableStateFlow(RepliAccountState())
    private var initialized = false
    private var auth: FirebaseAuth? = null
    private var backend: RepliAccountBackendClient? = null

    val state: StateFlow<RepliAccountState> = mutableState.asStateFlow()

    fun initialize(context: Context) {
        synchronized(initLock) {
            if (initialized) return
            initialized = true
            val configured = listOf(
                BuildConfig.REPLI_FIREBASE_API_KEY,
                BuildConfig.REPLI_FIREBASE_APP_ID,
                BuildConfig.REPLI_FIREBASE_PROJECT_ID,
            ).all(String::isNotBlank) && isBackendConfigured(BuildConfig.REPLI_BACKEND_URL)
            if (!configured) {
                mutableState.value = RepliAccountState(configured = false)
                return
            }
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.REPLI_FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.REPLI_FIREBASE_APP_ID)
                    .setProjectId(BuildConfig.REPLI_FIREBASE_PROJECT_ID)
                    .build()
                val app = FirebaseApp.getApps(context).firstOrNull { it.name == FIREBASE_APP_NAME }
                    ?: FirebaseApp.initializeApp(context, options, FIREBASE_APP_NAME)
                auth = FirebaseAuth.getInstance(app).also { firebaseAuth ->
                    firebaseAuth.addAuthStateListener { publishAccountState() }
                }
                backend = RepliAccountBackendClient.fromReplyEndpoint(BuildConfig.REPLI_BACKEND_URL)
                publishAccountState()
            } catch (_: Exception) {
                auth = null
                backend = null
                mutableState.value = RepliAccountState(
                    configured = false,
                    errorMessage = "Account sign-in is not configured correctly for this build.",
                )
            }
        }
    }

    suspend fun createAccount(email: String, password: String): Result<Unit> = accountOperation {
        val firebaseAuth = requireAuth()
        val result = firebaseAuth
            .createUserWithEmailAndPassword(normalizeEmail(email), validatePassword(password))
            .awaitResult()
        exchangeFor(result.user ?: error("Firebase did not return the new account."), forceRefresh = true)
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = accountOperation {
        val firebaseAuth = requireAuth()
        val result = firebaseAuth
            .signInWithEmailAndPassword(normalizeEmail(email), validatePassword(password))
            .awaitResult()
        exchangeFor(result.user ?: error("Firebase did not return the signed-in account."), forceRefresh = true)
    }

    suspend fun signInWithGoogle(activity: Activity): Result<Unit> = accountOperation {
        val clientId = BuildConfig.REPLI_GOOGLE_WEB_CLIENT_ID
        check(clientId.isNotBlank()) { "Google sign-in needs the Firebase web client ID in this build." }
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(clientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        check(credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google did not return a sign-in credential."
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val result = requireAuth().signInWithCredential(
            GoogleAuthProvider.getCredential(idToken, null),
        ).awaitResult()
        exchangeFor(result.user ?: error("Firebase did not return the Google account."), forceRefresh = true)
    }

    suspend fun signInWithApple(activity: Activity): Result<Unit> = accountOperation {
        val firebaseAuth = requireAuth()
        val provider = OAuthProvider.newBuilder("apple.com").build()
        val result = (firebaseAuth.pendingAuthResult
            ?: firebaseAuth.startActivityForSignInWithProvider(activity, provider)).awaitResult()
        exchangeFor(result.user ?: error("Firebase did not return the Apple account."), forceRefresh = true)
    }

    fun signOut() {
        auth?.signOut()
        RepliAccountSessionRepository.clear()
        publishAccountState()
    }

    suspend fun deleteAccount(password: String): Result<Unit> = accountOperation {
        val firebaseAuth = requireAuth()
        val user = firebaseAuth.currentUser ?: error("Sign in again before deleting the account.")
        val email = user.email ?: error("This account cannot be reauthenticated with a password.")
        user.reauthenticate(EmailAuthProvider.getCredential(email, validatePassword(password))).awaitCompletion()
        val firebaseToken = user.getIdToken(true).awaitResult().token
            ?: error("Firebase did not return an identity token.")
        requireBackend().deleteAccount(firebaseToken)
        firebaseAuth.signOut()
        RepliAccountSessionRepository.clear()
    }

    suspend fun bearerTokenForRequest(): String? {
        RepliAccountSessionRepository.bearerToken()?.let { return it }
        return operationMutex.withLock {
            RepliAccountSessionRepository.bearerToken() ?: try {
                exchangeFor(requireUser(), forceRefresh = false)
                RepliAccountSessionRepository.bearerToken()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                publishError(error)
                null
            }
        }
    }

    private suspend fun accountOperation(operation: suspend () -> Unit): Result<Unit> {
        mutableState.value = mutableState.value.copy(busy = true, errorMessage = null)
        return try {
            operationMutex.withLock { operation() }
            publishAccountState(busy = false, errorMessage = null)
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            publishAccountState(busy = false)
            throw cancellation
        } catch (cancellation: GetCredentialCancellationException) {
            publishAccountState(busy = false, errorMessage = null)
            Result.failure(cancellation)
        } catch (error: Throwable) {
            publishAccountState(busy = false, errorMessage = friendlyError(error))
            Result.failure(error)
        }
    }

    private suspend fun exchangeFor(user: FirebaseUser, forceRefresh: Boolean) {
        val firstToken = user.getIdToken(forceRefresh).awaitResult().token
            ?: error("Firebase did not return an identity token.")
        val repliToken = try {
            requireBackend().exchangeSession(firstToken)
        } catch (error: RepliAccountAuthException) {
            if (forceRefresh) throw error
            val refreshed = user.getIdToken(true).awaitResult().token
                ?: error("Firebase did not refresh the identity token.")
            requireBackend().exchangeSession(refreshed)
        }
        RepliAccountSessionRepository.acceptFromTrustedSignIn(repliToken).getOrThrow()
        publishAccountState(errorMessage = null)
    }

    private fun publishAccountState(busy: Boolean = mutableState.value.busy, errorMessage: String? = mutableState.value.errorMessage) {
        val user = auth?.currentUser
        mutableState.value = RepliAccountState(
            configured = auth != null && backend != null,
            busy = busy,
            signedIn = user != null,
            email = user?.email,
            errorMessage = errorMessage,
        )
        if (user == null) RepliAccountSessionRepository.clear()
    }

    private fun publishError(error: Throwable) {
        mutableState.value = mutableState.value.copy(errorMessage = friendlyError(error))
    }

    private fun requireAuth(): FirebaseAuth =
        auth ?: error("Firebase account sign-in is not configured for this build.")

    private fun requireUser(): FirebaseUser =
        requireAuth().currentUser ?: error("Sign in to enable cloud replies.")

    private fun requireBackend(): RepliAccountBackendClient =
        backend ?: error("The Repli account backend is not configured for this build.")

    private fun normalizeEmail(value: String): String = value.trim().also {
        require(it.length in 3..254 && '@' in it) { "Enter a valid email address." }
    }

    private fun validatePassword(value: String): String = value.also {
        require(it.length in 6..1_024) { "Password must contain at least 6 characters." }
    }

    private const val FIREBASE_APP_NAME = "repli-auth"
}

internal fun isBackendConfigured(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        val uri = java.net.URI(url)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }
}

internal fun friendlyError(error: Throwable): String = when (error) {
    is FirebaseAuthWeakPasswordException -> "Choose a stronger password with at least 6 characters."
    is FirebaseAuthInvalidCredentialsException -> "The email or password is not valid."
    is FirebaseAuthInvalidUserException -> "That account is unavailable."
    is FirebaseAuthUserCollisionException -> "An account already exists for that email."
    is FirebaseAuthException -> if (error.errorCode == "ERROR_OPERATION_NOT_ALLOWED") {
        "This sign-in method isn't enabled in Firebase yet."
    } else {
        "Account sign-in failed. Check your connection and try again."
    }
    is RepliRecentAuthRequiredException -> "Sign in again before deleting the account."
    is RepliAccountAuthException -> "Your account session was rejected. Sign in again."
    is RepliAccountBackendException -> error.message ?: "The account service is unavailable."
    is IllegalArgumentException, is IllegalStateException -> error.message ?: "Account setup could not finish."
    else -> "Account setup could not finish. Check your connection and try again."
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase operation failed."))
    }
}

private suspend fun Task<*>.awaitCompletion(): Unit = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) continuation.resume(Unit)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase operation failed."))
    }
}

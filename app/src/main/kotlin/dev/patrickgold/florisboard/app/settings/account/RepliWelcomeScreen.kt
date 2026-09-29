package dev.patrickgold.florisboard.app.settings.account

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.settings.repli.RepliAction
import dev.patrickgold.florisboard.app.settings.repli.RepliCard
import dev.patrickgold.florisboard.app.settings.repli.RepliLabel
import dev.patrickgold.florisboard.app.settings.repli.RepliPage
import dev.patrickgold.florisboard.app.settings.repli.RepliStyle
import dev.patrickgold.florisboard.repli.account.RepliFirebaseAccountManager
import dev.patrickgold.florisboard.repli.account.RepliWelcomePreferences
import kotlinx.coroutines.launch

@Composable
fun RepliWelcomeScreen() {
    val context = LocalContext.current
    val activity = context as Activity
    val navController = LocalNavController.current
    val account by RepliFirebaseAccountManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showEmail by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var attemptedSignIn by remember { mutableStateOf(false) }

    fun finishWelcome() {
        RepliWelcomePreferences.complete(context)
        navController.navigate(Routes.Settings.RepliHome) {
            popUpTo(Routes.Settings.RepliWelcome) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(account.signedIn, attemptedSignIn) {
        if (account.signedIn && !attemptedSignIn) finishWelcome()
    }

    RepliPage {
        Spacer(Modifier.height(32.dp))
        RepliLabel("Your words. Your call.", 32, RepliStyle.ink, bold = true)
        Spacer(Modifier.height(10.dp))
        RepliLabel("Sign in to save your chats and generate replies that sound like you.",
            16, RepliStyle.muted)
        Spacer(Modifier.height(28.dp))
        RepliCard {
            RepliLabel("Get started", 21, RepliStyle.ink, bold = true)
            Spacer(Modifier.height(16.dp))
            RepliAction("Continue with Google", {
                attemptedSignIn = true
                scope.launch {
                    if (RepliFirebaseAccountManager.signInWithGoogle(activity).isSuccess) finishWelcome()
                }
            }, enabled = account.configured && !account.busy)
            Spacer(Modifier.height(10.dp))
            RepliAction("Continue with Apple", {
                attemptedSignIn = true
                scope.launch {
                    if (RepliFirebaseAccountManager.signInWithApple(activity).isSuccess) finishWelcome()
                }
            }, filled = false, enabled = account.configured && !account.busy)
            Spacer(Modifier.height(10.dp))
            RepliAction(if (showEmail) "Hide email sign-in" else "Use email instead", {
                showEmail = !showEmail
            }, filled = false, enabled = account.configured && !account.busy)
            if (showEmail) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(email, { email = it }, label = { Text("Email") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(password, { password = it }, label = { Text("Password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                RepliAction("Sign in with email", {
                    attemptedSignIn = true
                    scope.launch {
                        if (RepliFirebaseAccountManager.signIn(email, password).isSuccess) finishWelcome()
                    }
                }, enabled = !account.busy)
                Spacer(Modifier.height(8.dp))
                RepliAction("Create account with email", {
                    attemptedSignIn = true
                    scope.launch {
                        if (RepliFirebaseAccountManager.createAccount(email, password).isSuccess) finishWelcome()
                    }
                }, filled = false, enabled = !account.busy)
            }
            account.errorMessage?.let { error ->
                Spacer(Modifier.height(12.dp))
                RepliLabel(error, 13, RepliStyle.ink)
            }
            if (!account.configured) {
                Spacer(Modifier.height(12.dp))
                RepliLabel("Cloud sign-in is unavailable in this build. You can still use the keyboard.",
                    13, RepliStyle.muted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Continue without an account", color = RepliStyle.accent,
            modifier = Modifier.fillMaxWidth().clickable { finishWelcome() }
                .padding(vertical = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        RepliLabel("Cloud replies need an account and internet. Typing and suggestions work on your phone.",
            12, RepliStyle.muted)
    }
}

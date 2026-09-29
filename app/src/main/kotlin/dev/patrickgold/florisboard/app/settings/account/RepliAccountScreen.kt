package dev.patrickgold.florisboard.app.settings.account

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.repli.account.RepliFirebaseAccountManager
import dev.patrickgold.jetpref.datastore.ui.Preference
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes

@Composable
fun RepliAccountScreen() = FlorisScreen {
    title = stringRes(R.string.repli_account__title)
    previewFieldVisible = false

    content {
        val accountState by RepliFirebaseAccountManager.state.collectAsState()
        val activity = LocalActivity.current
        val scope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        if (!accountState.configured) {
            Preference(
                icon = Icons.Default.AccountCircle,
                title = stringRes(R.string.repli_account__not_configured_title),
                summary = stringRes(R.string.repli_account__not_configured_summary),
                onClick = { },
            )
            if (BuildConfig.DEBUG) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Debug config status: backend=" +
                            if (BuildConfig.REPLI_BACKEND_URL.isNotBlank()) "set" else "missing" +
                            ", firebase=" +
                            if (BuildConfig.REPLI_FIREBASE_API_KEY.isNotBlank() &&
                                BuildConfig.REPLI_FIREBASE_APP_ID.isNotBlank() &&
                                BuildConfig.REPLI_FIREBASE_PROJECT_ID.isNotBlank()
                            ) "set" else "missing" +
                            ". Rebuild after filling firebase.local.properties; " +
                            "uninstall first if switching variants."
                    )
                }
            }
        } else {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(stringRes(R.string.repli_account__description))
            Spacer(Modifier.height(12.dp))
            if (accountState.signedIn) {
                Text(stringRes(R.string.repli_account__signed_in_as, "email" to (accountState.email ?: "")))
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { RepliFirebaseAccountManager.signOut() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringRes(R.string.repli_account__sign_out))
                }
            } else {
                Button(
                    onClick = { activity?.let { scope.launch { RepliFirebaseAccountManager.signInWithGoogle(it) } } },
                    enabled = !accountState.busy && activity != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue with Google") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { activity?.let { scope.launch { RepliFirebaseAccountManager.signInWithApple(it) } } },
                    enabled = !accountState.busy && activity != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continue with Apple") }
                Spacer(Modifier.height(18.dp))
                Text("Or use email")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringRes(R.string.repli_account__email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringRes(R.string.repli_account__password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { RepliFirebaseAccountManager.signIn(email, password) } },
                    enabled = !accountState.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringRes(R.string.repli_account__sign_in))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { scope.launch { RepliFirebaseAccountManager.createAccount(email, password) } },
                    enabled = !accountState.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringRes(R.string.repli_account__create_account))
                }
            }
            accountState.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it)
            }
            }
        }

        if (accountState.configured && accountState.signedIn) {
            Preference(
                icon = Icons.Default.Logout,
                title = stringRes(R.string.repli_account__sign_out),
                summary = accountState.email,
                onClick = { RepliFirebaseAccountManager.signOut() },
            )
        }
    }
}

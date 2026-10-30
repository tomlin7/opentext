package com.tom7.opentext.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tom7.opentext.data.Workspace
import com.tom7.opentext.data.WorkspaceStore
import com.tom7.opentext.data.normalizeServerUrl
import com.tom7.opentext.data.probeRemote
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.Grey500
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

@Composable
fun ConnectScreen(
    client: OkHttpClient,
    store: WorkspaceStore,
    onBack: () -> Unit,
    onConnected: (Workspace) -> Unit,
) {
    var url by remember { mutableStateOf(store.remoteUrl.orEmpty()) }
    var password by remember { mutableStateOf(store.remotePassword) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grey1100)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Grey100)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Connect",
            style = MaterialTheme.typography.titleLarge,
            color = Grey100,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Use the same address and password OpenRemote uses. OpenText asks the server for its project folder.",
            style = MaterialTheme.typography.bodyMedium,
            color = Grey500,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Server address") },
            placeholder = { Text("http://192.168.1.10:4096") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                val normalized = normalizeServerUrl(url)
                if (normalized == null) {
                    error = "Enter a valid http or https address."
                    return@Button
                }
                busy = true
                error = null
                scope.launch {
                    runCatching { probeRemote(normalized, password, client) }
                        .onSuccess { workspace ->
                            store.remoteUrl = normalized
                            store.remotePassword = password
                            store.remoteDirectory = workspace.root
                            onConnected(workspace)
                        }
                        .onFailure { error = it.message ?: "Could not connect." }
                    busy = false
                }
            },
            enabled = !busy && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Grey1100)
                    Spacer(Modifier.size(10.dp))
                    Text("Connecting…")
                }
            } else {
                Text("Connect", color = Grey1100)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Tip: OpenRemote can hand a project straight to OpenText from a session header.",
            style = MaterialTheme.typography.labelSmall,
            color = Grey500,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "OpenCode server · basic auth",
            style = MaterialTheme.typography.labelSmall,
            color = Accent,
        )
    }
}

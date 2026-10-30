package com.tom7.opentext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tom7.opentext.data.LocalWorkspace
import com.tom7.opentext.data.RemoteWorkspace
import com.tom7.opentext.data.SettingsStore
import com.tom7.opentext.data.Workspace
import com.tom7.opentext.data.WorkspaceStore
import com.tom7.opentext.data.normalizeServerUrl
import com.tom7.opentext.data.probeRemote
import com.tom7.opentext.ui.connect.ConnectScreen
import com.tom7.opentext.ui.editor.EditorScreen
import com.tom7.opentext.ui.home.HomeScreen
import com.tom7.opentext.ui.settings.SettingsScreen
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.OpenTextTheme
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** A project handed over by OpenRemote's session header. */
data class RemoteOpen(
    val url: String,
    val password: String,
    val directory: String,
)

class MainActivity : ComponentActivity() {
    private val incoming = MutableStateFlow<RemoteOpen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incoming.value = remoteFrom(intent)
        setContent {
            val remote by incoming.collectAsStateWithLifecycle()
            OpenTextRoot(
                remote = remote,
                onConsumed = { incoming.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        remoteFrom(intent)?.let { incoming.value = it }
    }
}

private fun remoteFrom(intent: Intent?): RemoteOpen? {
    if (intent == null) return null
    if (intent.action != ACTION_OPEN_REMOTE) return null
    val url = intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() } ?: return null
    val directory = intent.getStringExtra(EXTRA_DIRECTORY)?.takeIf { it.isNotBlank() } ?: return null
    return RemoteOpen(url, intent.getStringExtra(EXTRA_PASSWORD).orEmpty(), directory)
}

const val ACTION_OPEN_REMOTE = "com.tom7.opentext.OPEN_REMOTE"
const val EXTRA_URL = "url"
const val EXTRA_PASSWORD = "password"
const val EXTRA_DIRECTORY = "directory"

@Composable
fun OpenTextRoot(
    remote: RemoteOpen?,
    onConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { WorkspaceStore(context) }
    val settings = remember { SettingsStore(context) }
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    var workspace by remember { mutableStateOf<Workspace?>(null) }
    var showConnect by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Create PtyClient when workspace is remote
    val ptyClient = remember(workspace) {
        (workspace as? RemoteWorkspace)?.createPtyClient()
    }

    fun openLocal(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        runCatching { LocalWorkspace(context, uri) }
            .onSuccess { opened ->
                store.lastLocalTree = uri.toString()
                store.lastLocalLabel = opened.label
                workspace = opened
            }
    }

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) openLocal(uri)
    }

    fun openFolder() {
        val lastUri = store.lastLocalTree
        val initialUri = if (lastUri != null) Uri.parse(lastUri) else null
        folderLauncher.launch(initialUri)
    }

    LaunchedEffect(remote) {
        val value = remote ?: return@LaunchedEffect
        val normalized = normalizeServerUrl(value.url)
        if (normalized != null) {
            try {
                // Verify connection works by probing, but use the directory from the intent
                probeRemote(normalized, value.password, client)
                val remoteWorkspace = RemoteWorkspace(normalized, value.password, value.directory, client)
                store.remoteUrl = normalized
                store.remotePassword = value.password
                store.remoteDirectory = value.directory
                workspace = remoteWorkspace
            } catch (e: Exception) {
                connectionError = e.message ?: "Could not connect to server"
            }
        } else {
            connectionError = "Invalid server URL"
        }
        onConsumed()
    }

    OpenTextTheme {
        Box(Modifier.fillMaxSize().background(Grey1100)) {
            when {
                showSettings -> SettingsScreen(
                    settingsStore = settings,
                    onBack = { showSettings = false }
                )
                workspace != null -> EditorScreen(
                    workspace = workspace!!,
                    ptyClient = ptyClient,
                    onExit = { workspace = null },
                    onOpenSettings = { showSettings = true }
                )
                showConnect -> ConnectScreen(
                    client = client,
                    store = store,
                    onBack = { showConnect = false },
                    onConnected = { workspace = it }
                )
                else -> HomeScreen(
                    recentLocalLabel = store.lastLocalLabel,
                    recentRemoteDirectory = store.remoteDirectory,
                    connectionError = connectionError,
                    onDismissError = { connectionError = null },
                    onOpenFolder = { openFolder() },
                    onConnect = { showConnect = true },
                    onReopenLocal = {
                        store.lastLocalTree?.let { uriString ->
                            openLocal(Uri.parse(uriString))
                        }
                    },
                    onReopenRemote = {
                        val url = store.remoteUrl
                        val directory = store.remoteDirectory
                        if (url != null && directory != null) {
                            workspace = RemoteWorkspace(url, store.remotePassword, directory, client)
                        } else {
                            showConnect = true
                        }
                    },
                    onSettings = { showSettings = true }
                )
            }
        }
    }
}

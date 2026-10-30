package com.tom7.opentext.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One entry inside a workspace, using `/`-separated paths relative to the workspace root. */
data class Entry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
)

/**
 * A folder OpenText can browse and edit. Local workspaces back onto a SAF tree; remote
 * workspaces speak the OpenCode server filesystem API over the same tunnel OpenRemote uses.
 */
interface Workspace {
    /** Stable identity across recompositions, used to key caches. */
    val id: String

    /** Short name shown in the editor header. */
    val label: String

    /** Human-readable root, shown under the name. */
    val root: String

    val isRemote: Boolean

    suspend fun list(path: String): List<Entry>

    suspend fun readText(path: String): String

    suspend fun writeText(path: String, text: String)
}

// ---------------------------------------------------------------------------
// Local (Storage Access Framework)
// ---------------------------------------------------------------------------

class LocalWorkspace(
    context: Context,
    private val treeUri: Uri,
) : Workspace {
    private val resolver = context.applicationContext.contentResolver
    private val tree = DocumentFile.fromTreeUri(context, treeUri)
        ?: error("This folder can no longer be opened.")

    override val id: String = "local:$treeUri"
    override val label: String = tree.name ?: "Folder"
    override val root: String = treeUri.toString()
    override val isRemote: Boolean = false

    override suspend fun list(path: String): List<Entry> = withContext(Dispatchers.IO) {
        val directory = resolve(path) ?: return@withContext emptyList()
        directory.listFiles()
            .map { file ->
                val name = file.name ?: return@map null
                Entry(join(path, name), name, file.isDirectory)
            }
            .filterNotNull()
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    override suspend fun readText(path: String): String = withContext(Dispatchers.IO) {
        val file = resolve(path) ?: error("File not found: $path")
        val bytes = resolver.openInputStream(file.uri)?.use { it.readBytes() }
            ?: error("Could not open $path")
        decodeText(bytes)
    }

    override suspend fun writeText(path: String, text: String): Unit = withContext(Dispatchers.IO) {
        val existing = resolve(path)
        val uri = existing?.uri ?: create(path)?.uri ?: error("Could not create $path")
        resolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("Could not write $path")
    }

    private fun resolve(path: String): DocumentFile? {
        if (path.isBlank()) return tree
        var current: DocumentFile = tree
        for (segment in path.split('/')) {
            current = current.findFile(segment) ?: return null
        }
        return current
    }

    private fun create(path: String): DocumentFile? {
        val parentPath = path.substringBeforeLast('/', "")
        val name = path.substringAfterLast('/')
        val parent = resolve(parentPath) ?: return null
        return parent.createFile("text/plain", name)
    }
}

// ---------------------------------------------------------------------------
// Remote (OpenCode server, usually through OpenRemote's tunnel)
// ---------------------------------------------------------------------------

@Serializable
private data class RemoteEntry(val path: String = "", val type: String = "file")

@Serializable
private data class RemoteList(val data: List<RemoteEntry> = emptyList())

@Serializable
private data class RemoteLocation(val directory: String = "")

class RemoteWorkspace(
    private val baseUrl: String,
    password: String,
    override val root: String,
    private val client: OkHttpClient,
) : Workspace {
    private val authorization = Credentials.basic("opencode", password)

    override val id: String = "remote:$baseUrl|$root"
    override val label: String = root.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifBlank { "Remote" }
    override val isRemote: Boolean = true

    override suspend fun list(path: String): List<Entry> {
        val builder = (baseUrl + "/api/fs/list").toHttpUrl().newBuilder()
        if (path.isNotBlank()) builder.addQueryParameter("path", path)
        builder.addQueryParameter("location[directory]", root)
        val body = execute(Request.Builder().url(builder.build()).auth().get().build())
        val parsed = AppJson.decodeFromString<RemoteList>(body)
        return parsed.data
            .mapNotNull { entry ->
                val normalized = entry.path.replace('\\', '/').trimStart('/')
                val name = normalized.substringAfterLast('/')
                if (name.isBlank()) null else Entry(normalized, name, entry.type == "directory")
            }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    override suspend fun readText(path: String): String {
        val url = (baseUrl + "/api/fs/read/" + encodePath(path)).toHttpUrl().newBuilder()
            .addQueryParameter("location[directory]", root)
            .build()
        val bytes = executeBytes(Request.Builder().url(url).auth().get().build())
        return decodeText(bytes)
    }

    override suspend fun writeText(path: String, text: String) {
        val url = (baseUrl + "/api/experimental/fs/write").toHttpUrl().newBuilder()
            .addQueryParameter("path", path)
            .addQueryParameter("location[directory]", root)
            .build()
        val body = text.toByteArray().toRequestBody("application/octet-stream".toMediaType())
        execute(Request.Builder().url(url).auth().post(body).build())
    }

    private fun Request.Builder.auth() = header("Authorization", authorization)

    private suspend fun execute(request: Request): String {
        val response = client.newCall(request).await()
        return withContext(Dispatchers.IO) {
            response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw IOException(summary(it, body))
                body
            }
        }
    }

    private suspend fun executeBytes(request: Request): ByteArray {
        val response = client.newCall(request).await()
        return withContext(Dispatchers.IO) {
            response.use {
                if (!it.isSuccessful) throw IOException(summary(it, it.body?.string().orEmpty()))
                it.body?.bytes() ?: ByteArray(0)
            }
        }
    }

    private fun summary(response: Response, body: String): String {
        val detail = body.take(240).ifBlank { response.message }
        return when (response.code) {
            401, 403 -> "The server rejected the connection. Check the password."
            404 -> "Not found on the server."
            else -> "$detail (${response.code})"
        }
    }

    private fun encodePath(path: String): String =
        path.split('/').joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

fun join(parent: String, name: String): String = if (parent.isBlank()) name else "$parent/$name"

private fun decodeText(bytes: ByteArray): String {
    if (bytes.any { it == 0.toByte() }) throw IOException("This looks like a binary file.")
    return String(bytes, Charsets.UTF_8)
}

fun normalizeServerUrl(input: String): String? {
    var value = input.trim()
    if (value.isEmpty()) return null
    if (!value.contains("://")) value = "http://$value"
    val url = runCatching { value.toHttpUrl() }.getOrNull() ?: return null
    if (url.scheme != "http" && url.scheme != "https") return null
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
    if (url.query != null || url.fragment != null) return null
    return url.toString().trimEnd('/')
}

suspend fun probeRemote(baseUrl: String, password: String, client: OkHttpClient): RemoteWorkspace {
    val request = Request.Builder()
        .url((baseUrl + "/api/location").toHttpUrl())
        .header("Authorization", Credentials.basic("opencode", password))
        .header("Accept", "application/json")
        .get()
        .build()
    val body = client.newCall(request).await().use { response ->
        val text = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw IOException(
                if (response.code == 401 || response.code == 403) "The server rejected the connection."
                else text.take(200).ifBlank { "Could not reach the server (${response.code})." },
            )
        }
        text
    }
    val directory = runCatching { AppJson.decodeFromString<RemoteLocation>(body).directory }.getOrDefault("")
    if (directory.isBlank()) throw IOException("The server did not report a folder.")
    return RemoteWorkspace(baseUrl, password, directory, client)
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) else response.close()
        }
    })
}

package com.tom7.opentext.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Client for managing PTY (pseudo-terminal) sessions on the OpenCode server.
 */
class PtyClient(
    private val baseUrl: String,
    password: String,
    private val client: OkHttpClient,
) {
    private val authorization = Credentials.basic("opencode", password)

    private val _sessions = MutableStateFlow<List<PtySession>>(emptyList())
    val sessions: StateFlow<List<PtySession>> = _sessions.asStateFlow()

    /**
     * List all PTY sessions.
     */
    suspend fun listSessions(): List<PtySession> = withContext(Dispatchers.IO) {
        val url = (baseUrl + "/api/pty").toHttpUrl().newBuilder().build()
        val body = execute(Request.Builder().url(url).auth().get().build())
        val parsed = AppJson.decodeFromString<List<PtySession>>(body)
        _sessions.value = parsed
        parsed
    }

    /**
     * Create a new PTY session.
     */
    suspend fun createSession(request: CreatePtyRequest): PtySession = withContext(Dispatchers.IO) {
        val url = (baseUrl + "/api/pty").toHttpUrl().newBuilder().build()
        val body = AppJson.encodeToString(CreatePtyRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())
        val response = execute(Request.Builder().url(url).auth().post(body).build())
        val session = AppJson.decodeFromString<PtySession>(response)
        _sessions.value = _sessions.value + session
        session
    }

    /**
     * Get a specific PTY session by ID.
     */
    suspend fun getSession(id: String): PtySession = withContext(Dispatchers.IO) {
        val url = (baseUrl + "/api/pty/$id").toHttpUrl().newBuilder().build()
        val body = execute(Request.Builder().url(url).auth().get().build())
        AppJson.decodeFromString<PtySession>(body)
    }

    /**
     * Send input to a PTY session.
     */
    suspend fun sendInput(sessionId: String, input: PtyInputRequest): Unit = withContext(Dispatchers.IO) {
        val url = (baseUrl + "/api/pty/$sessionId/input").toHttpUrl().newBuilder().build()
        val body = AppJson.encodeToString(PtyInputRequest.serializer(), input)
            .toRequestBody("application/json".toMediaType())
        execute(Request.Builder().url(url).auth().post(body).build())
    }

    /**
     * Get output from a PTY session.
     */
    suspend fun getOutput(sessionId: String): PtyOutputResponse = withContext(Dispatchers.IO) {
        val url = (baseUrl + "/api/pty/$sessionId/output").toHttpUrl().newBuilder().build()
        val body = execute(Request.Builder().url(url).auth().get().build())
        AppJson.decodeFromString<PtyOutputResponse>(body)
    }

    /**
     * Delete a PTY session.
     */
    suspend fun deleteSession(id: String): Unit = withContext(Dispatchers.IO) {
        val url = (baseUrl + "/api/pty/$id").toHttpUrl().newBuilder().build()
        execute(Request.Builder().url(url).auth().delete().build())
        _sessions.value = _sessions.value.filter { it.id != id }
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

    private fun summary(response: Response, body: String): String {
        val detail = body.take(240).ifBlank { response.message }
        return when (response.code) {
            401, 403 -> "The server rejected the connection. Check the password."
            404 -> "PTY session not found."
            else -> "$detail (${response.code})"
        }
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
}

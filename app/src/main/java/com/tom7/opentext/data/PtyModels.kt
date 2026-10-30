package com.tom7.opentext.data

import kotlinx.serialization.Serializable

/**
 * Represents a PTY (pseudo-terminal) session managed by the OpenCode server.
 */
@Serializable
data class PtySession(
    val id: String,
    val command: String,
    val args: List<String> = emptyList(),
    val cwd: String,
    val pid: Int? = null,
    val status: PtyStatus = PtyStatus.RUNNING,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class PtyStatus {
    RUNNING,
    EXITED,
    ERROR
}

/**
 * Request to create a new PTY session.
 */
@Serializable
data class CreatePtyRequest(
    val command: String = "bash",
    val args: List<String> = emptyList(),
    val cwd: String,
    val env: Map<String, String> = emptyMap()
)

/**
 * Request to send input to a PTY session.
 */
@Serializable
data class PtyInputRequest(
    val data: String
)

/**
 * Response from PTY output stream.
 */
@Serializable
data class PtyOutputResponse(
    val data: String,
    val exitCode: Int? = null
)

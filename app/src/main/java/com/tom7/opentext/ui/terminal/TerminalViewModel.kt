package com.tom7.opentext.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom7.opentext.data.CreatePtyRequest
import com.tom7.opentext.data.PtyClient
import com.tom7.opentext.data.PtyInputRequest
import com.tom7.opentext.data.PtySession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TerminalViewModel(
    private val ptyClient: PtyClient,
    private val workspaceRoot: String,
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<PtySession>>(emptyList())
    val sessions: StateFlow<List<PtySession>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<PtySession?>(null)
    val activeSession: StateFlow<PtySession?> = _activeSession.asStateFlow()

    private val _terminalOutput = MutableStateFlow<String>("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val sessions = ptyClient.listSessions()
                _sessions.value = sessions
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load sessions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectSession(session: PtySession?) {
        _activeSession.value = session
        if (session != null) {
            loadOutput(session.id)
        } else {
            _terminalOutput.value = ""
        }
    }

    fun createSession() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val request = CreatePtyRequest(
                    command = "bash",
                    cwd = workspaceRoot
                )
                val session = ptyClient.createSession(request)
                _sessions.value = _sessions.value + session
                selectSession(session)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create session"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendInput(input: String) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            try {
                ptyClient.sendInput(session.id, PtyInputRequest(input))
                // Add input to output display
                _terminalOutput.value += input
                // Refresh output after sending input
                loadOutput(session.id)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send input"
            }
        }
    }

    fun loadOutput(sessionId: String) {
        viewModelScope.launch {
            try {
                val response = ptyClient.getOutput(sessionId)
                _terminalOutput.value = response.data
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load output"
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                ptyClient.deleteSession(sessionId)
                _sessions.value = _sessions.value.filter { it.id != sessionId }
                if (_activeSession.value?.id == sessionId) {
                    selectSession(null)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete session"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

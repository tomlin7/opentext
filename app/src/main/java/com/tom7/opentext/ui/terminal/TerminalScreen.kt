package com.tom7.opentext.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tom7.opentext.data.PtySession
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1000
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onDismiss: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var showSessionList by remember { mutableStateOf(false) }
    
    LaunchedEffect(error) {
        error?.let {
            // Error will be shown via Snackbar or dialog
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Grey1000,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp)
        ) {
            // Header with session selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terminal",
                    style = MaterialTheme.typography.titleLarge,
                    color = Grey100
                )
                Row {
                    IconButton(onClick = { showSessionList = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Session",
                            tint = Grey100
                        )
                    }
                    if (activeSession != null) {
                        IconButton(onClick = { viewModel.deleteSession(activeSession!!.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Session",
                                tint = Grey100
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Session indicator
            if (activeSession != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Session: ${activeSession!!.id.take(8)}...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Grey500
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = activeSession!!.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (activeSession!!.status == "running") Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "No active session",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Grey500
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Terminal output
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Color(0xFF0D0D0D),
                shape = RoundedCornerShape(4.dp)
            ) {
                if (terminalOutput.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeSession == null) "No active session.\nCreate a new session to start." else "Terminal output will appear here...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Grey600,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        item {
                            Text(
                                text = terminalOutput,
                                style = MaterialTheme.typography.bodySmall,
                                color = Grey100,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Input field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter command...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Grey100,
                        unfocusedTextColor = Grey100,
                        focusedBorderColor = Grey500,
                        unfocusedBorderColor = Grey600,
                        focusedPlaceholderColor = Grey600,
                        unfocusedPlaceholderColor = Grey600,
                        cursorColor = Grey100
                    ),
                    enabled = activeSession != null && !isLoading,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendInput(inputText + "\n")
                            inputText = ""
                        }
                    },
                    enabled = activeSession != null && inputText.isNotBlank() && !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (activeSession != null && inputText.isNotBlank()) Grey100 else Grey600
                    )
                }
            }
        }
    }
    
    // Session list dialog
    if (showSessionList) {
        AlertDialog(
            onDismissRequest = { showSessionList = false },
            title = { Text("PTY Sessions") },
            text = {
                if (sessions.isEmpty()) {
                    Text("No active sessions")
                } else {
                    LazyColumn {
                        items(sessions) { session ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    viewModel.selectSession(session)
                                    showSessionList = false
                                },
                                color = if (session.id == activeSession?.id) Color(0xFF2D2D2D) else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Session: ${session.id.take(8)}...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Grey100
                                        )
                                        Text(
                                            text = session.cwd,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Grey500
                                        )
                                    }
                                    Text(
                                        text = session.status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (session.status == "running") Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createSession()
                    showSessionList = false
                }) {
                    Text("Create New")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionList = false }) {
                    Text("Close")
                }
            },
            containerColor = Grey1000,
            titleContentColor = Grey100,
            textContentColor = Grey100
        )
    }
}

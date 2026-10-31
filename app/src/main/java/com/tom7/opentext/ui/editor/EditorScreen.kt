package com.tom7.opentext.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom7.opentext.data.Entry
import com.tom7.opentext.data.PtyClient
import com.tom7.opentext.data.Workspace
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1000
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey600
import com.tom7.opentext.ui.terminal.TerminalScreen
import com.tom7.opentext.ui.terminal.TerminalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    workspace: Workspace,
    ptyClient: PtyClient? = null,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: EditorViewModel = viewModel(key = "editor:${workspace.id}")
    LaunchedEffect(workspace.id) { vm.open(workspace) }

    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var caretLine by remember { mutableStateOf(1) }
    var caretColumn by remember { mutableStateOf(1) }
    var quickOpen by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    var index by remember(workspace.id) { mutableStateOf<List<Entry>>(emptyList()) }
    var exitConfirm by remember { mutableStateOf(false) }

    val requestExit = {
        if (vm.files.any { it.modified }) exitConfirm = true else onExit()
    }

    BackHandler { requestExit() }

    LaunchedEffect(vm.toast) {
        vm.toast?.let {
            snackbar.showSnackbar(it)
            vm.consumeToast()
        }
    }

    val active = vm.active

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Grey1000,
                modifier = Modifier.width(300.dp),
            ) {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    // Minimal drawer header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = workspace.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = Grey100,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (workspace.isRemote) workspace.root else "On this device",
                                style = MaterialTheme.typography.labelSmall,
                                color = Grey500,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (ptyClient != null) {
                            IconButton(
                                onClick = {
                                    showTerminal = true
                                    scope.launch { drawer.close() }
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Terminal,
                                    contentDescription = "Terminal",
                                    tint = Grey500,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { vm.refresh() },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = Grey500,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        FileTree(
                            rows = vm.rows,
                            activePath = active?.path,
                            onToggle = { row -> vm.toggle(Entry(row.path, row.name, row.isDirectory)) },
                            onOpen = { row ->
                                vm.openFile(Entry(row.path, row.name, isDirectory = false))
                                scope.launch { drawer.close() }
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    // Settings at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenSettings()
                                scope.launch { drawer.close() }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = Grey500,
                        )
                    }
                }
                if (vm.loading && vm.rows.isEmpty()) {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.labelSmall,
                        color = Grey600,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            containerColor = Grey1100,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbar) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Grey1000,
                        contentColor = Grey100,
                        actionColor = Accent,
                    )
                }
            },
            bottomBar = {
                MiniStatusBar(
                    filename = active?.name ?: "",
                    language = active?.language?.label ?: "",
                    modified = active?.modified == true,
                    line = caretLine,
                    column = caretColumn,
                    onLanguageClick = { showLanguagePicker = true },
                )
            },
        ) { inner ->
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(inner)) {
                if (vm.files.isNotEmpty()) {
                    MiniTabStrip(
                        files = vm.files,
                        activeIndex = vm.activeIndex,
                        onSelect = { vm.select(it) },
                        onClose = { vm.requestClose(it) },
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {},
                                onVerticalDrag = { change, dragAmount ->
                                    if (dragAmount > 100f) {
                                        showCommandPalette = true
                                    }
                                }
                            )
                        }
                ) {
                    if (active == null) {
                        EmptyEditor()
                    } else {
                        CodeEditor(
                            file = active,
                            onTextChange = { vm.updateActive(it) },
                            onCaret = { line, column ->
                                caretLine = line
                                caretColumn = column
                            },
                        )
                    }
                }
            }
        }
    }

    if (quickOpen) {
        LaunchedEffect(quickOpen) { index = vm.buildIndex() }
        QuickOpenSheet(
            index = index,
            onDismiss = { quickOpen = false },
            onOpen = { entry ->
                quickOpen = false
                vm.openFile(entry)
            },
        )
    }

    if (showCommandPalette) {
        LaunchedEffect(showCommandPalette) { index = vm.buildIndex() }
        CommandPalette(
            fileIndex = index,
            onDismiss = { showCommandPalette = false },
            onFileOpen = { entry ->
                showCommandPalette = false
                vm.openFile(entry)
            },
            onCommand = { command ->
                showCommandPalette = false
                when (command) {
                    "save" -> vm.save()
                    "quickopen" -> quickOpen = true
                    "terminal" -> if (ptyClient != null) showTerminal = true
                }
            },
            onSettingChange = { setting, value ->
                showCommandPalette = false
                when (setting) {
                    "settings" -> onOpenSettings()
                }
            },
        )
    }

    if (showTerminal && ptyClient != null) {
        val terminalViewModel = remember(ptyClient, workspace.root) {
            TerminalViewModel(ptyClient, workspace.root)
        }
        TerminalScreen(
            viewModel = terminalViewModel,
            onDismiss = { showTerminal = false }
        )
    }

    if (showLanguagePicker && vm.active != null) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text("Select Language") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Language.entries.forEach { language ->
                        Text(
                            text = language.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.setLanguage(vm.activeIndex, language)
                                    showLanguagePicker = false
                                }
                                .padding(vertical = 12.dp),
                            color = if (language == vm.active!!.language) Accent else Grey100
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguagePicker = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Grey1000,
        )
    }

    if (exitConfirm) {
        AlertDialog(
            onDismissRequest = { exitConfirm = false },
            title = { Text("Unsaved changes") },
            text = { Text("Save before closing this workspace?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveAll()
                    exitConfirm = false
                    onExit()
                }) { Text("Save all") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        exitConfirm = false
                        onExit()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { exitConfirm = false }) { Text("Cancel") }
                }
            },
            containerColor = Grey1000,
        )
    }

    vm.pendingClose?.let {
        AlertDialog(
            onDismissRequest = { vm.dismissClose() },
            title = { Text("Discard changes?") },
            text = { Text("This file has unsaved changes.") },
            confirmButton = {
                TextButton(onClick = { vm.confirmClose() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissClose() }) { Text("Keep editing") }
            },
            containerColor = Grey1000,
        )
    }

    vm.error?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.clearError() },
            title = { Text("Could not open folder") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { vm.clearError() }) { Text("OK") } },
            containerColor = Grey1000,
        )
    }
}

@Composable
private fun MiniTabStrip(
    files: List<OpenFile>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
) {
    var expandedTab by remember { mutableStateOf<Int?>(null) }
    
    LazyRow(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Grey1100)
    ) {
        itemsIndexed(files, key = { _, file -> file.path }) { index, file ->
            val selected = index == activeIndex
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .combinedClickable(
                        onClick = { onSelect(index) },
                        onLongClick = { expandedTab = index }
                    )
                    .padding(horizontal = 12.dp)
                    .drawBehind {
                        if (selected) {
                            drawLine(
                                color = Accent,
                                start = Offset(0f, size.height - 1.5f),
                                end = Offset(size.width, size.height - 1.5f),
                                strokeWidth = 3f
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Grey100 else Grey500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            
            DropdownMenu(
                expanded = expandedTab == index,
                onDismissRequest = { expandedTab = null }
            ) {
                DropdownMenuItem(
                    text = { Text("Close") },
                    onClick = {
                        onClose(index)
                        expandedTab = null
                    }
                )
            }
        }
    }
}

@Composable
private fun MiniStatusBar(
    filename: String,
    language: String,
    modified: Boolean,
    line: Int,
    column: Int,
    onLanguageClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(22.dp)
            .background(Grey1100)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (modified && filename.isNotEmpty()) {
            Box(
                Modifier
                    .size(4.dp)
                    .background(Accent, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
        }
        if (filename.isNotEmpty()) {
            Text(
                text = filename,
                style = MaterialTheme.typography.labelSmall,
                color = Grey500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        if (language.isNotEmpty()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = Grey600,
                modifier = Modifier.clickable(onClick = onLanguageClick)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = "Ln $line, Col $column",
            style = MaterialTheme.typography.labelSmall,
            color = Grey600,
        )
    }
}

@Composable
private fun EmptyEditor() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "opentext",
                style = MaterialTheme.typography.titleMedium,
                color = Grey600,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pull down for commands",
                style = MaterialTheme.typography.labelSmall,
                color = Grey600,
            )
        }
    }
}

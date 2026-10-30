package com.tom7.opentext.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tom7.opentext.data.Entry
import com.tom7.opentext.data.Workspace
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.BorderDarkBase
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1000
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey600
import com.tom7.opentext.ui.theme.Grey900
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    workspace: Workspace,
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
    var showQuickActions by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(14) }
    var wordWrap by remember { mutableStateOf(false) }
    var index by remember(workspace.id) { mutableStateOf<List<Entry>>(emptyList()) }
    var exitConfirm by remember { mutableStateOf(false) }

    // Leaving the workspace is the only action that can drop unsaved edits, so guard just that.
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
                modifier = Modifier.width(320.dp),
            ) {
                Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = workspace.label,
                                style = MaterialTheme.typography.titleMedium,
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
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = Grey500)
                        }
                    }
                    HorizontalDivider(color = BorderDarkBase)
                    
                    // Quick Actions Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuickActions = !showQuickActions }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quick Actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = Grey100
                        )
                        Icon(
                            imageVector = if (showQuickActions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (showQuickActions) "Collapse" else "Expand",
                            tint = Grey500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (showQuickActions) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            // Font Size Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Font Size",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Grey500
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (fontSize > 8) fontSize-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Remove,
                                            contentDescription = "Decrease font size",
                                            tint = Grey100,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "${fontSize}sp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Grey100,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { if (fontSize < 24) fontSize++ },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = "Increase font size",
                                            tint = Grey100,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Word Wrap Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Word Wrap",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Grey500
                                )
                                androidx.compose.material3.Switch(
                                    checked = wordWrap,
                                    onCheckedChange = { wordWrap = it },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Accent,
                                        checkedTrackColor = Accent.copy(alpha = 0.5f),
                                        uncheckedThumbColor = Grey500,
                                        uncheckedTrackColor = Grey500.copy(alpha = 0.3f)
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Settings Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSettings() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = Grey500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Grey100,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = BorderDarkBase)
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
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                if (vm.loading && vm.rows.isEmpty()) {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Grey500,
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
                        containerColor = Grey900,
                        contentColor = Grey100,
                        actionColor = Accent,
                    )
                }
            },
            bottomBar = {
                Column {
                    // Bottom action bar for mobile
                    ActionBar(
                        onSave = { vm.save() },
                        onQuickOpen = { quickOpen = true },
                        onUndo = { /* TODO: implement undo */ },
                        onRedo = { /* TODO: implement redo */ },
                        canSave = active?.modified == true && !vm.saving,
                    )
                    StatusBar(
                        language = active?.language?.label ?: "Plain text",
                        modified = active?.modified == true,
                        line = caretLine,
                        column = caretColumn,
                        onLanguageClick = { showLanguagePicker = true }
                    )
                }
            },
        ) { inner ->
            Column(Modifier.fillMaxSize().padding(inner)) {
                TopBar(
                    title = workspace.label,
                    subtitle = active?.path ?: "No file open",
                    modified = active?.modified == true,
                    saving = vm.saving,
                    onMenu = { scope.launch { drawer.open() } },
                    onQuickOpen = { quickOpen = true },
                    onSave = { vm.save() },
                    onExit = requestExit,
                )
                if (vm.files.isNotEmpty()) {
                    TabStrip(
                        files = vm.files,
                        activeIndex = vm.activeIndex,
                        onSelect = { vm.select(it) },
                        onClose = { vm.requestClose(it) },
                    )
                }
                Box(Modifier.weight(1f)) {
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
private fun TopBar(
    title: String,
    subtitle: String,
    modified: Boolean,
    saving: Boolean,
    onMenu: () -> Unit,
    onQuickOpen: () -> Unit,
    onSave: () -> Unit,
    onExit: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Grey1000).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, contentDescription = "Files", tint = Grey100)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Grey100,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Grey500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onQuickOpen) {
                Icon(Icons.Outlined.Search, contentDescription = "Go to file", tint = Grey500)
            }
            IconButton(onClick = onSave, enabled = modified && !saving) {
                Icon(
                    Icons.Outlined.Save,
                    contentDescription = "Save",
                    tint = when {
                        saving -> Grey600
                        modified -> Accent
                        else -> Grey600
                    },
                )
            }
            IconButton(onClick = onExit) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close workspace", tint = Grey500)
            }
        }
        HorizontalDivider(color = BorderDarkBase)
    }
}

@Composable
private fun TabStrip(
    files: List<OpenFile>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Grey1000)) {
        LazyRow(Modifier.fillMaxWidth().height(44.dp)) {
            itemsIndexed(files, key = { _, file -> file.path }) { index, file ->
                val selected = index == activeIndex
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .background(if (selected) Grey1100 else Grey1000)
                        .clickable { onSelect(index) }
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) Grey100 else Grey500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (file.modified) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(5.dp).background(Accent, CircleShape))
                    }
                    IconButton(
                        onClick = { onClose(index) },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close ${file.name}",
                            tint = Grey600,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (index < files.lastIndex) {
                    Box(Modifier.width(1.dp).height(18.dp).background(BorderDarkBase))
                }
            }
        }
        HorizontalDivider(color = BorderDarkBase)
    }
}

@Composable
private fun StatusBar(
    language: String,
    modified: Boolean,
    line: Int,
    column: Int,
    onLanguageClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Grey1000)) {
        HorizontalDivider(color = BorderDarkBase)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = Grey500,
                maxLines = 1,
                modifier = Modifier.clickable(onClick = onLanguageClick)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Ln $line, Col $column",
                style = MaterialTheme.typography.labelSmall,
                color = Grey500,
            )
            Text(
                text = if (modified) "Unsaved" else "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = if (modified) Accent else Grey600,
            )
        }
    }
}

@Composable
private fun ActionBar(
    onSave: () -> Unit,
    onQuickOpen: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canSave: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Grey1000)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onUndo,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Undo,
                contentDescription = "Undo",
                tint = Grey500,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onRedo,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Redo,
                contentDescription = "Redo",
                tint = Grey500,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onQuickOpen,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Quick open",
                tint = Grey500,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Outlined.Save,
                contentDescription = "Save",
                tint = if (canSave) Accent else Grey600,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyEditor() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "opentext",
                style = MaterialTheme.typography.titleLarge,
                color = Grey100,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Open a file from the sidebar",
                style = MaterialTheme.typography.bodySmall,
                color = Grey500,
            )
        }
    }
}

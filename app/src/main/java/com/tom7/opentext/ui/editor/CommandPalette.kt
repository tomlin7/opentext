package com.tom7.opentext.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WrapText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tom7.opentext.data.Entry
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey700
import com.tom7.opentext.ui.theme.Grey900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPalette(
    fileIndex: List<Entry>,
    onDismiss: () -> Unit,
    onFileOpen: (Entry) -> Unit,
    onCommand: (String) -> Unit,
    onSettingChange: (String, Any?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val tabs = listOf("Files", "Commands", "Settings")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Type a command or search…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Grey100,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> FilesTab(query, fileIndex, onFileOpen)
                1 -> CommandsTab(query, onCommand)
                2 -> SettingsTab(query, onSettingChange)
            }
        }
    }
}

@Composable
private fun FilesTab(
    query: String,
    index: List<Entry>,
    onFileOpen: (Entry) -> Unit,
) {
    val matches = remember(query, index) {
        if (query.isBlank()) index.take(60)
        else index
            .asSequence()
            .map { it to score(it.path, query) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(60)
            .toList()
    }

    if (matches.isEmpty()) {
        Text(
            text = if (index.isEmpty()) "No files indexed yet." else "No match.",
            style = MaterialTheme.typography.bodyMedium,
            color = Grey500,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    } else {
        LazyColumn {
            items(matches, key = { it.path }) { entry ->
                FileItem(entry) { onFileOpen(entry) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun FileItem(entry: Entry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = Grey700,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                color = Grey100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val parent = entry.path.substringBeforeLast('/', "")
            if (parent.isNotBlank()) {
                Text(
                    text = parent,
                    style = MaterialTheme.typography.labelSmall,
                    color = Grey500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CommandsTab(
    query: String,
    onCommand: (String) -> Unit,
) {
    val commands = remember {
        listOf(
            CommandItem("save", "Save File", Icons.Outlined.Code),
            CommandItem("quickopen", "Quick Open File", Icons.Outlined.Search),
            CommandItem("find", "Find in File", Icons.Outlined.Search),
            CommandItem("replace", "Find and Replace", Icons.Outlined.Search),
            CommandItem("goto", "Go to Line", Icons.Outlined.Code),
            CommandItem("format", "Format Document", Icons.Outlined.Code),
        )
    }

    val matches = remember(query, commands) {
        if (query.isBlank()) commands
        else commands.filter { 
            it.id.contains(query, ignoreCase = true) || 
            it.label.contains(query, ignoreCase = true) 
        }
    }

    if (matches.isEmpty()) {
        Text(
            text = "No commands found.",
            style = MaterialTheme.typography.bodyMedium,
            color = Grey500,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    } else {
        LazyColumn {
            items(matches, key = { it.id }) { command ->
                CommandItem(command) { onCommand(command.id) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

private data class CommandItem(val id: String, val label: String, val icon: ImageVector)

@Composable
private fun CommandItem(command: CommandItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            command.icon,
            contentDescription = null,
            tint = Grey700,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = command.label,
            style = MaterialTheme.typography.bodyMedium,
            color = Grey100,
        )
    }
}

@Composable
private fun SettingsTab(
    query: String,
    onSettingChange: (String, Any?) -> Unit,
) {
    val settings = remember {
        listOf(
            SettingItem("fontSize", "Font Size", Icons.Outlined.FontDownload),
            SettingItem("wordWrap", "Word Wrap", Icons.Outlined.WrapText),
            SettingItem("theme", "Theme", Icons.Outlined.Settings),
        )
    }

    val matches = remember(query, settings) {
        if (query.isBlank()) settings
        else settings.filter { it.id.contains(query, ignoreCase = true) || it.label.contains(query, ignoreCase = true) }
    }

    if (matches.isEmpty()) {
        Text(
            text = "No settings found.",
            style = MaterialTheme.typography.bodyMedium,
            color = Grey500,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    } else {
        LazyColumn {
            items(matches, key = { it.id }) { setting ->
                SettingItem(setting, onSettingChange)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

private data class SettingItem(val id: String, val label: String, val icon: ImageVector)

@Composable
private fun SettingItem(setting: SettingItem, onClick: (String, Any?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(setting.id, null) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            setting.icon,
            contentDescription = null,
            tint = Grey700,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = setting.label,
            style = MaterialTheme.typography.bodyMedium,
            color = Grey100,
        )
    }
}

/** Scores a fuzzy match: exact filename hits rank above scattered path hits. */
private fun score(path: String, query: String): Int {
    val lowerPath = path.lowercase()
    val lowerQuery = query.lowercase()
    val name = path.substringAfterLast('/').lowercase()
    return when {
        name == lowerQuery -> 1000
        name.startsWith(lowerQuery) -> 800 - name.length
        name.contains(lowerQuery) -> 600 - name.length
        lowerPath.contains(lowerQuery) -> 400 - lowerPath.length
        else -> subsequence(lowerPath, lowerQuery)
    }
}

private fun subsequence(text: String, query: String): Int {
    var index = 0
    var gaps = 0
    for (char in text) {
        if (index < query.length && char == query[index]) index++ else if (index > 0) gaps++
    }
    return if (index == query.length) 200 - gaps / 2 else 0
}

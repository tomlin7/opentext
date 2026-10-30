package com.tom7.opentext.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tom7.opentext.data.Entry
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey700
import com.tom7.opentext.ui.theme.Grey900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickOpenSheet(
    index: List<Entry>,
    onDismiss: () -> Unit,
    onOpen: (Entry) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
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
                placeholder = { Text("Go to file…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
            if (matches.isEmpty()) {
                Text(
                    text = if (index.isEmpty()) "No files indexed yet." else "No match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Grey500,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
            LazyColumn {
                items(matches, key = { it.path }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(entry) }
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
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
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

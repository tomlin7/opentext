package com.tom7.opentext.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.Grey1000
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey600
import com.tom7.opentext.ui.theme.Grey900

@Composable
fun FileTree(
    rows: List<TreeRow>,
    activePath: String?,
    onToggle: (TreeRow) -> Unit,
    onOpen: (TreeRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(rows, key = { it.path }) { row ->
            val selected = !row.isDirectory && row.path == activePath
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 1.dp)
                    .background(
                        if (selected) Grey900 else Grey1000,
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { if (row.isDirectory) onToggle(row) else onOpen(row) }
                    .padding(start = (6 + row.depth * 14).dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row.isDirectory) {
                    Icon(
                        imageVector = when {
                            row.expanded -> Icons.Outlined.KeyboardArrowDown
                            else -> Icons.AutoMirrored.Outlined.KeyboardArrowRight
                        },
                        contentDescription = null,
                        tint = Grey500,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = if (row.expanded) Icons.Outlined.FolderOpen else Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = Grey500,
                        modifier = Modifier.size(15.dp),
                    )
                } else {
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                        contentDescription = null,
                        tint = if (selected) Accent else Grey600,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    text = row.name,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = if (selected) Grey100 else if (row.isDirectory) Grey100 else Grey100.copy(alpha = 0.86f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

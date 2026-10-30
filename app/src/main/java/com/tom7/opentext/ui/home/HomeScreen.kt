package com.tom7.opentext.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey700

@Composable
fun HomeScreen(
    recentLocalLabel: String?,
    recentRemoteDirectory: String?,
    onOpenFolder: () -> Unit,
    onConnect: () -> Unit,
    onReopenLocal: () -> Unit,
    onReopenRemote: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grey1100)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "opentext",
                color = Grey100,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Grey500)
            }
        }

        Spacer(Modifier.height(32.dp))

        ActionRow(
            icon = Icons.Outlined.FolderOpen,
            title = "Open folder",
            onClick = onOpenFolder,
        )
        Spacer(Modifier.height(8.dp))
        ActionRow(
            icon = Icons.Outlined.CloudQueue,
            title = "Connect to server",
            onClick = onConnect,
        )

        if (recentLocalLabel != null || recentRemoteDirectory != null) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Recent",
                style = MaterialTheme.typography.labelSmall,
                color = Grey700,
            )
            Spacer(Modifier.height(8.dp))
            if (recentLocalLabel != null) {
                RecentRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = recentLocalLabel,
                    onClick = onReopenLocal,
                )
                if (recentRemoteDirectory != null) Spacer(Modifier.height(4.dp))
            }
            if (recentRemoteDirectory != null) {
                RecentRow(
                    icon = Icons.Outlined.CloudQueue,
                    title = recentRemoteDirectory.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifBlank { recentRemoteDirectory },
                    onClick = onReopenRemote,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Grey500, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Grey100)
    }
}

@Composable
private fun RecentRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Grey700, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Grey100,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Grey700,
            modifier = Modifier.size(16.dp),
        )
    }
}

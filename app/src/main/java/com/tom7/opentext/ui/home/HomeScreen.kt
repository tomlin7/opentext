package com.tom7.opentext.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
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
import com.tom7.opentext.ui.theme.Accent
import com.tom7.opentext.ui.theme.BorderDarkBase
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1000
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.Grey500
import com.tom7.opentext.ui.theme.Grey600

@Composable
fun HomeScreen(
    recentLocalLabel: String?,
    recentRemoteDirectory: String?,
    onOpenFolder: () -> Unit,
    onConnect: () -> Unit,
    onReopenLocal: () -> Unit,
    onReopenRemote: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grey1100)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = "opentext",
            color = Grey100,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "A quiet, fast editor for your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = Grey500,
        )

        Spacer(Modifier.height(36.dp))

        ActionRow(
            icon = Icons.Outlined.FolderOpen,
            title = "Open folder",
            subtitle = "Browse files on this device",
            onClick = onOpenFolder,
        )
        Spacer(Modifier.height(10.dp))
        ActionRow(
            icon = Icons.Outlined.CloudQueue,
            title = "Connect to server",
            subtitle = "Edit a project through OpenRemote",
            onClick = onConnect,
        )

        if (recentLocalLabel != null || recentRemoteDirectory != null) {
            Spacer(Modifier.height(36.dp))
            Text(
                text = "RECENT",
                style = MaterialTheme.typography.labelSmall,
                color = Grey600,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (recentLocalLabel != null) {
                RecentRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = recentLocalLabel,
                    subtitle = "On this device",
                    onClick = onReopenLocal,
                )
                if (recentRemoteDirectory != null) Spacer(Modifier.height(6.dp))
            }
            if (recentRemoteDirectory != null) {
                RecentRow(
                    icon = Icons.Outlined.CloudQueue,
                    title = recentRemoteDirectory.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\').ifBlank { recentRemoteDirectory },
                    subtitle = recentRemoteDirectory,
                    onClick = onReopenRemote,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = "Works offline. Connects through the OpenCode server.",
            style = MaterialTheme.typography.labelSmall,
            color = Grey600,
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Grey1000, RoundedCornerShape(12.dp))
            .border(1.dp, BorderDarkBase, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Grey100)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Grey500)
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Grey600,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RecentRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Grey600, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Grey100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Grey600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Grey600,
            modifier = Modifier.size(16.dp),
        )
    }
}

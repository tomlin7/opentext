package com.tom7.opentext.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tom7.opentext.data.SettingsStore
import com.tom7.opentext.ui.theme.Grey100
import com.tom7.opentext.ui.theme.Grey1100
import com.tom7.opentext.ui.theme.Grey500

@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Grey1100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Grey100
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = Grey100
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSection("Editor") {
                SwitchSetting(
                    title = "Word wrap",
                    description = "Wrap long lines instead of scrolling horizontally",
                    checked = settingsStore.wordWrap,
                    onCheckedChange = { settingsStore.wordWrap = it }
                )
                Spacer(Modifier.height(8.dp))
                SwitchSetting(
                    title = "Show line numbers",
                    description = "Display line numbers in the gutter",
                    checked = settingsStore.showLineNumbers,
                    onCheckedChange = { settingsStore.showLineNumbers = it }
                )
                Spacer(Modifier.height(8.dp))
                SliderSetting(
                    title = "Font size",
                    value = settingsStore.fontSize,
                    onValueChange = { settingsStore.fontSize = it },
                    valueRange = 10f..24f,
                    steps = 13
                )
                Spacer(Modifier.height(8.dp))
                SliderSetting(
                    title = "Tab size",
                    value = settingsStore.tabSize.toFloat(),
                    onValueChange = { settingsStore.tabSize = it.toInt() },
                    valueRange = 2f..8f,
                    steps = 5
                )
            }

            Spacer(Modifier.height(24.dp))

            SettingsSection("Appearance") {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Grey100
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dark (coming soon: light theme)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Grey500
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Grey500,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Grey100
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Grey500
            )
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Grey100
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = Grey500
            )
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

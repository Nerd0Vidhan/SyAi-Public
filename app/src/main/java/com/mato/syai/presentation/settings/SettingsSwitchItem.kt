package com.mato.syai.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {

            Text(title, fontWeight = FontWeight.SemiBold)

            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
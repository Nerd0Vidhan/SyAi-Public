package com.mato.syai.presentation.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mato.syai.ui.theme.WhitePurple


@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {

    val scale by animateFloatAsState(
        targetValue = if (checked) 1.05f else 1f,
        animationSpec = spring(),
        label = ""
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            icon,
            null,
            tint = WhitePurple,
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(title, fontWeight = FontWeight.SemiBold, color = WhitePurple)

            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            colors =  SwitchDefaults.colors(

                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                checkedBorderColor = Color.Transparent,

                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = Color.Transparent,

                disabledCheckedThumbColor = MaterialTheme.colorScheme.outline,
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
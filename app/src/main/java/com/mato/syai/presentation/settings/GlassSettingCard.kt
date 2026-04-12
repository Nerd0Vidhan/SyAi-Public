package com.mato.syai.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mato.syai.utils.GlassEffect

@Composable
fun GlassSettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {

    GlassEffect(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        glassTintColor = Color(0x11000000).copy(alpha = 0.5f)
    ) {

        Column(
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            content()
        }
    }
}
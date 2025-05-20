package com.mato.syai.notes.presentation.topbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContextMenu(
    onPageSettingsClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isMenuExpanded by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .width(120.dp)
            .heightIn(min = 70.dp, max = 160.dp)
            .padding(5.dp)
            .verticalScroll(scrollState)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Save", fontSize = 12.sp)
            Text("Page Settings", fontSize = 12.sp, modifier = Modifier.clickable { onPageSettingsClick() })
            Text("More Options", fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContextMenuPrivew(){
    ContextMenu(onPageSettingsClick = {})
}
package com.mato.syai.note.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

@Composable
fun rememberImagePicker(onImagePicked: (Uri) -> Unit): () -> Unit {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImagePicked(it) }
    }

    return {
        launcher.launch("image/*")
    }
}
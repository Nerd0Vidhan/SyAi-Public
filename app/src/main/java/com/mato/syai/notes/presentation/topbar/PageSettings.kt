package com.mato.syai.notes.presentation.topbar

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.mato.syai.notes.core.model.CanvasBackground
import com.mato.syai.notes.core.utils.hexToColor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PageSettingsMenu(
    onBackgroundChange: (CanvasBackground) -> Unit
) {
    val scrollState = rememberScrollState()
    var colorInput by remember { mutableStateOf("#FFFFFF") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val imagePermissionState = rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            onBackgroundChange(CanvasBackground.Image(it.toString()))
        }
    }

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(400.dp)
            .verticalScroll(scrollState)
            .padding(8.dp)
    ) {
        Column {

            Text(text = "Page Settings", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(12.dp))

            Text(text = "Enter Hex Color")

            BasicTextField(
                value = colorInput,
                onValueChange = { colorInput = it },
                modifier = Modifier
                    .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                    .padding(8.dp)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(try {
                        hexToColor(colorInput)
                    } catch (e: Exception) {
                        Color.White
                    })
                    .border(1.dp, Color.Black)
            )

            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                try {
                    onBackgroundChange(CanvasBackground.Solid(hexToColor(colorInput)))
                } catch (e: Exception) {
                    Toast.makeText(context, "Invalid color code", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Apply Color")
            }

            Spacer(Modifier.height(24.dp))

            Text(text = "Set Image Background")

            Button(onClick = {
                when (imagePermissionState.status) {
                    is PermissionStatus.Granted -> imagePickerLauncher.launch("image/*")
                    is PermissionStatus.Denied -> imagePermissionState.launchPermissionRequest()
                }
            }) {
                Text("Choose Image")
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun OptionView(){
    PageSettingsMenu(){}
}
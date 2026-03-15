package com.mato.syai.notifications

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat

@Preview
@Composable
fun NotificationExample() {
    val context = LocalContext.current

    val hasPermission = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission.value = isGranted
        if (isGranted) {
            sendNotification(
                context = context,
                title = "Live Notification",
                message = "jaa be bhaakkk (from permission granted)"
            )
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val notifications = Notifications()
    notifications.createNotificationChannel(context)

    Button(
        modifier = Modifier.fillMaxSize(),
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (hasPermission.value) {
                    sendNotification(
                        context = context,
                        title = "Live Notification",
                        message = "jaa be bhaakkk"
                    )
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                sendNotification(
                    context = context,
                    title = "Live Notification",
                    message = "balle balle."
                )
                Toast.makeText(context, "Notification shown!", Toast.LENGTH_SHORT).show()
            }
        }
    ) {
        Text("Send Notification")
    }
}

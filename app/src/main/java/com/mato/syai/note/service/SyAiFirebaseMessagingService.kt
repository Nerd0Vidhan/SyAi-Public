package com.mato.syai.note.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mato.syai.MainActivity
import com.mato.syai.R
import com.mato.syai.note.ai.image.ImageGenerationEvent
import com.mato.syai.note.ai.image.ImageGenerationEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SyAiFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var eventBus: ImageGenerationEventBus

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val jobId = data["jobId"] ?: ""
            val status = data["status"] ?: ""
            val noteId = data["noteId"]?.toLongOrNull() ?: -1L
            val pageNo = data["pageNo"]?.toIntOrNull() ?: 0
            val imageUrl = data["imageUrl"] ?: ""

            Log.d("FCM", "JobId: $jobId, Status: $status, NoteId: $noteId")

            if (status == "COMPLETED") {
                eventBus.tryEmit(ImageGenerationEvent.JobCompleted(noteId, jobId, imageUrl, pageNo))
            } else if (status == "FAILED") {
                eventBus.tryEmit(ImageGenerationEvent.JobFailed(noteId, jobId, data["error"] ?: "Unknown error"))
            }

            // Always show notification if app is in background or even if in foreground (user choice)
            // But usually we show it if noteId doesn't match current open note.
            // For now, let's show it if it's COMPLETED.
            if (status == "COMPLETED") {
                showNotification(noteId, jobId)
            }
        }
    }

    private fun showNotification(noteId: Long, jobId: String) {
        val channelId = "image_generation_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Image Generation",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NOTE_ID", noteId)
            putExtra("JOB_ID", jobId)
            putExtra("OPEN_NOTE", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, noteId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.syai_square_vector_logo)
            .setContentTitle("Image Ready")
            .setContentText("Your generated image is ready to be added to your note.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(noteId.toInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // Optionally send to server if you want to keep track of all devices
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}

package com.mato.syai.note.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mato.syai.R

class AIOptimizerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 9876
        const val CHANNEL_ID = "ai_optimization_service_channel"
        const val ACTION_STOP = "com.mato.syai.ACTION_STOP"
        const val ACTION_STOP_FROM_USER = "com.mato.syai.ACTION_STOP_FROM_USER"

        fun updateNotification(context: Context, status: String, progress: Int) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = createNotification(context, status, progress)
            manager.notify(NOTIFICATION_ID, notification)
        }

        private fun createNotification(context: Context, status: String, progress: Int): Notification {
            createNotificationChannel(context)
            val stopIntent = Intent(context, AIOptimizerService::class.java).apply {
                action = ACTION_STOP_FROM_USER
            }
            val stopPendingIntent = android.app.PendingIntent.getService(
                context,
                1,
                stopIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("SyAi Local Optimizer")
                .setContentText(status)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Optimization", stopPendingIntent)
                .build()
        }

        private fun createNotificationChannel(context: Context) {
            val name = "AI Local Optimizer Service"
            val descriptionText = "Monitors background AI model loops and verification steps"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_FROM_USER) {
            AIOptimizerOrchestrator.stopSession(this, "Stopped by user")
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            val status = intent.getStringExtra("status") ?: "Finished"
            stopForeground(STOP_FOREGROUND_REMOVE)
            
            // Show complete/final update notification
            val finalNotification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SyAi Local Optimizer")
                .setContentText("Status: $status")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .build()
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID + 1, finalNotification)
            
            stopSelf()
            return START_NOT_STICKY
        }

        val prompt = intent?.getStringExtra("prompt") ?: "Generating enhanced layout..."
        startForeground(NOTIFICATION_ID, createNotification(this, prompt, 10))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

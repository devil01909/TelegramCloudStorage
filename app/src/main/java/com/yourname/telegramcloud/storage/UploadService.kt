package com.yourname.telegramcloud.storage

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yourname.telegramcloud.MainActivity
import com.yourname.telegramcloud.R
import com.yourname.telegramcloud.utils.Constants
import kotlinx.coroutines.*

class UploadService : Service() {
    
    companion object {
        private const val TAG = "UploadService"
        private const val CHANNEL_ID = "upload_service_channel"
        private const val NOTIFICATION_ID = Constants.NOTIFICATION_ID_UPLOAD
        
        fun start(context: Context) {
            val intent = Intent(context, UploadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, UploadService::class.java)
            context.stopService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var uploadManager: UploadManager
    
    override fun onCreate() {
        super.onCreate()
        uploadManager = UploadManager.getInstance(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Initializing...", 0f))
        
        // Observe upload queue and update notification
        serviceScope.launch {
            uploadManager.currentUpload.collect { task ->
                task?.let {
                    updateNotification(it.fileName, it.progress)
                }
            }
        }
        
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upload Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing upload progress"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, progress: Float): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val progressPercent = (progress * 100).toInt()
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Uploading... $progressPercent%")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progressPercent, progress == 0f)
            .build()
    }
    
    private fun updateNotification(title: String, progress: Float) {
        val notification = createNotification(title, progress)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
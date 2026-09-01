package com.bhusanket.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

internal object CriticalAlerts {
    private const val channelId = "critical-risk"
    private const val notificationId = 9001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            channelId,
            "Critical landslide risk",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Immediate evacuation warnings for critical monitored zones"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notify(context: Context, name: String, score: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("critical_warning", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.bhusanket.app.R.drawable.ic_bhusanket)
            .setContentTitle("CRITICAL LANDSLIDE RISK")
            .setContentText("Evacuate now. Move to a green safe shelter marker.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$name: risk score $score. Leave steep slopes and river channels. Ambulance ETA 08 min · Ground response ETA 12 min."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .build()
        try { NotificationManagerCompat.from(context).notify(notificationId, notification) } catch (_: SecurityException) { }
    }

    fun openSms(context: Context, name: String, score: Int) {
        val message = "BHUSANKET CRITICAL WARNING: $name risk score $score/100. EVACUATE NOW. Move to a green safe shelter. Ambulance ETA 08 min. Ground response ETA 12 min."
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply { putExtra("sms_body", message) }
        context.startActivity(intent)
    }
}

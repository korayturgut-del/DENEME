package com.yasli.yardimci.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yasli.yardimci.R
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.screens.AlarmActivity

/**
 * Alarm zamanı gelince çalışır. Android 10+ arka plan aktivite başlatma kısıtı
 * nedeniyle ekranı doğrudan açamaz -> tam ekran uyarı (FSI) bildirimi yayınlar.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        fun intent(context: Context, baslik: String): Intent =
            Intent(context, AlarmReceiver::class.java).putExtra("baslik", baslik)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val baslik = intent.getStringExtra("baslik") ?: "Hatırlatma"

        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("baslik", baslik)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val contentPi = PendingIntent.getActivity(
            context, 999, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fsiPi = PendingIntent.getActivity(
            context, 1000, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val kanal = "alarm"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(kanal, "Alarmlar", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val n = Notification.Builder(context, kanal)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("İLAÇ SAATİ / HATIRLATMA")
            .setContentText(baslik)
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(contentPi)
            .setFullScreenIntent(fsiPi, true)
            .setAutoCancel(true)
            .build()
        nm.notify(2000, n)

        Tts.init(context)
        Tts.konus("İlaç saati. $baslik.")
    }
}

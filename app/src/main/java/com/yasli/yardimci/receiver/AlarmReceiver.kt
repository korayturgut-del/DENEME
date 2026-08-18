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
import com.yasli.yardimci.service.AlarmScheduler
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.screens.AlarmActivity

/**
 * Alarm zamanı gelince çalışır. Android 10+ arka plan aktivite başlatma kısıtı
 * nedeniyle ekranı doğrudan açamaz -> tam ekran uyarı (FSI) bildirimi yayınlar.
 * Tekrarlı alarmlar (W2) bir sonraki güne yeniden planlanır.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        fun intent(
            context: Context,
            baslik: String,
            saat: String,
            tekrar: String,
            tip: String,
            requestCode: Int
        ): Intent = Intent(context, AlarmReceiver::class.java)
            .putExtra("baslik", baslik)
            .putExtra("saat", saat)
            .putExtra("tekrar", tekrar)
            .putExtra("tip", tip)
            .putExtra("requestCode", requestCode)
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val baslik = intent.getStringExtra("baslik") ?: "Hatırlatma"
            val saat = intent.getStringExtra("saat") ?: ""
            val tekrar = intent.getStringExtra("tekrar") ?: "bugun"
            val tip = intent.getStringExtra("tip") ?: "hatirlatici"
            val requestCode = intent.getIntExtra("requestCode", -1)

            // W2: tekrarlı alarm kendini bir sonraki güne yeniden planlar
            if (tekrar != "bugun" && saat.isNotBlank() && requestCode >= 0) {
                AlarmScheduler.sonrakiIcinYenidenPlanla(context, requestCode, baslik, saat, tekrar, tip)
            }

            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("baslik", baslik)
                putExtra("tip", tip)
                putExtra("requestCode", requestCode)
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
        } catch (e: Exception) {
            // Bildirim izni yoksa veya FSI engelliyse sessiz kal; çökmek yok (Y1/Y2)
        }
    }
}

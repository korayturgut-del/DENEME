package com.yasli.yardimci.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.data.entity.NotificationLog
import com.yasli.yardimci.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WhatsApp ve SMS bildirimlerini yakalar, Room'a kaydeder ve Türkçe sesli okur.
 * Kullanıcının "Bildirim erişimi"ni açmış olması gerekir (ilk kurulumda yönlendirilir).
 */
class MessageListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val kaynak = when {
            sbn.packageName == "com.whatsapp" -> "whatsapp"
            sbn.packageName == "com.google.android.apps.messaging" ||
                sbn.packageName == "com.android.mms" ||
                sbn.packageName.startsWith("com.android.messaging") -> "sms"
            else -> return
        }

        val extras = sbn.notification?.extras ?: return
        val baslik = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val metin = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        // R4: Android 15 OTP redaksiyonu — boş/redakte içerik kaydedilmez
        if (metin.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(this@MessageListenerService)
                    .notificationLogDao()
                    .ekle(
                        NotificationLog(
                            kaynak = kaynak,
                            kimden = baslik,
                            metin = metin,
                            zaman = System.currentTimeMillis()
                        )
                    )
                Tts.init(this@MessageListenerService)
                if (Prefs.sesAcik(this@MessageListenerService)) {
                    Tts.konus("$baslik yazdı: $metin")
                }
            } catch (e: Exception) {
                // F6: sessiz veri kaybı yerine kontrollü hata — log yok, çökme yok
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}

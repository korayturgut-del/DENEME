package com.yasli.yardimci.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * İlaç/hatırlatıcı alarmlarını setAlarmClock ile planlar. Bu yöntem Android 12+/14+
 * exact alarm kısıtlarından muaf kalır ve durum çubuğunda alarm ikonu gösterir.
 * requestCode: hatırlatıcılar için 10000+id, ilaçlar için 20000+id.
 */
object AlarmScheduler {

    fun planla(context: Context, requestCode: Int, baslik: String, saat: String) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            val parcalar = saat.split(":")
            set(Calendar.HOUR_OF_DAY, parcalar[0].toIntOrNull() ?: 8)
            set(Calendar.MINUTE, parcalar.getOrNull(1)?.toIntOrNull() ?: 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            AlarmReceiver.intent(context, baslik),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
    }

    fun iptal(context: Context, requestCode: Int) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            AlarmReceiver.intent(context, ""),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmMgr.cancel(pi)
    }

    // Yeniden başlatma / saat değişimi sonrası tüm aktif alarmları yeniden kurar.
    fun hepsiniYenidenPlanla(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(context)
            db.reminderDao().aktif().first().forEach {
                planla(context, (10000 + it.id).toInt(), it.baslik, it.saat)
            }
            db.medicineDao().aktif().first().forEach {
                planla(context, (20000 + it.id).toInt(), it.ad, it.saat)
            }
        }
    }
}

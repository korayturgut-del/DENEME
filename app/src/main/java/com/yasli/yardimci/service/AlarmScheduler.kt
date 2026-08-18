package com.yasli.yardimci.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * İlaç/hatırlatıcı alarmlarını setAlarmClock ile planlar. Android 14+ exact alarm
 * kısıtı nedeniyle canScheduleExactAlarms kontrol edilir; izinsizken false döner.
 * requestCode: hatırlatıcılar için 10000+id, ilaçlar için 20000+id.
 * tekrar: bugun | hergun | haftaici (W2).
 */
object AlarmScheduler {

    const val REMINDER_TABAN = 10000
    const val MEDICINE_TABAN = 20000

    fun planla(
        context: Context,
        requestCode: Int,
        baslik: String,
        saat: String,
        tekrar: String = "bugun",
        tip: String = "hatirlatici"
    ): Boolean {
        if (!exactAlarmVarMi(context)) return false
        return try {
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
                context, requestCode,
                AlarmReceiver.intent(context, baslik, saat, tekrar, tip, requestCode),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun iptal(context: Context, requestCode: Int) {
        try {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context, requestCode,
                AlarmReceiver.intent(context, "", "", "bugun", "hatirlatici", requestCode),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.cancel(pi)
        } catch (_: Exception) {
        }
    }

    // W2: tetiklenen tekrarlı alarmı bir sonraki güne yeniden planlar
    fun sonrakiIcinYenidenPlanla(
        context: Context,
        requestCode: Int,
        baslik: String,
        saat: String,
        tekrar: String,
        tip: String
    ): Boolean {
        if (tekrar == "bugun") return false
        return try {
            if (!exactAlarmVarMi(context)) return false
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val cal = Calendar.getInstance().apply {
                val parcalar = saat.split(":")
                set(Calendar.HOUR_OF_DAY, parcalar[0].toIntOrNull() ?: 8)
                set(Calendar.MINUTE, parcalar.getOrNull(1)?.toIntOrNull() ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (tekrar == "hergun") {
                    add(Calendar.DAY_OF_YEAR, 1)
                } else { // haftaici
                    do {
                        add(Calendar.DAY_OF_YEAR, 1)
                    } while (
                        get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    )
                }
            }
            val pi = PendingIntent.getBroadcast(
                context, requestCode,
                AlarmReceiver.intent(context, baslik, saat, tekrar, tip, requestCode),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Yeniden başlatma / saat değişimi sonrası tüm aktif alarmları yeniden kurar (F3: güvenli).
    fun hepsiniYenidenPlanla(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.get(context)
            db.reminderDao().aktif().first().forEach {
                runCatching {
                    planla(context, (REMINDER_TABAN + it.id).toInt(), it.baslik, it.saat, it.tekrar, "hatirlatici")
                }
            }
            db.medicineDao().aktif().first().forEach {
                runCatching {
                    planla(context, (MEDICINE_TABAN + it.id).toInt(), it.ad, it.saat, it.gunler, "ilac")
                }
            }
        }
    }

    private fun exactAlarmVarMi(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
}

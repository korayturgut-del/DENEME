package com.yasli.yardimci.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yasli.yardimci.service.AlarmScheduler

// Telefon yeniden başlayınca tüm aktif alarmları yeniden planlar.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.hepsiniYenidenPlanla(context)
    }
}

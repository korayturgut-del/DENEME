package com.yasli.yardimci.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yasli.yardimci.service.AlarmScheduler

// Saat / zaman dilimi değişince alarmları yeniden planlar.
class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.hepsiniYenidenPlanla(context)
    }
}

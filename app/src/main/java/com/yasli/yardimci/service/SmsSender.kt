package com.yasli.yardimci.service

import android.telephony.SmsManager

/**
 * SMS gönderme (varsayılan abonelik). İzinsiz/hatalı durumda false döner, çökmez (S7).
 */
object SmsSender {

    fun gonder(telefon: String, metin: String): Boolean = try {
        SmsManager.getDefault().sendTextMessage(telefon, null, metin, null, null)
        true
    } catch (e: Exception) {
        false
    }
}

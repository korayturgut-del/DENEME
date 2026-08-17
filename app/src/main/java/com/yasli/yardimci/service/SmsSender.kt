package com.yasli.yardimci.service

import android.telephony.SmsManager

/**
 * SMS gönderme (varsayılan abonelik). Gönderme için varsayılan SMS uygulaması olmak GEREKMEZ.
 */
object SmsSender {

    fun gonder(telefon: String, metin: String) {
        val sms = SmsManager.getDefault()
        sms.sendTextMessage(telefon, null, metin, null, null)
    }
}

package com.yasli.yardimci.util

import android.content.Context

/**
 * Basit ayarlar (SharedPreferences). SOS numarası ve ses tercihi burada tutulur.
 */
object Prefs {

    private const val DOSYA = "yasli_ayarlar"

    private fun sp(context: Context) =
        context.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)

    fun sosNo(context: Context): String =
        sp(context).getString("sos_no", "") ?: ""

    fun setSosNo(context: Context, no: String) {
        sp(context).edit().putString("sos_no", no).apply()
    }

    fun sesAcik(context: Context): Boolean =
        sp(context).getBoolean("ses_acik", true)

    fun setSes(context: Context, acik: Boolean) {
        sp(context).edit().putBoolean("ses_acik", acik).apply()
    }
}

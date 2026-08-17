package com.yasli.yardimci.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Türkçe sesli okuma (tekil). Uygulama genelinde tek örnek kullanılır.
 */
object Tts {

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var hazir = false

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            hazir = status == TextToSpeech.SUCCESS
            if (hazir) {
                tts?.language = Locale("tr", "TR")
            }
        }
    }

    fun konus(text: String) {
        if (hazir) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yasli-tts")
        }
    }

    fun kapat() {
        tts?.shutdown()
        tts = null
        hazir = false
    }
}

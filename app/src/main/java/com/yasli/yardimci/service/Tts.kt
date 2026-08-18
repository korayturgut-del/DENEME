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

    // F11: cümle cümle konuşur — ilk cümle anında, kalanlar sırayla
    fun konusSirali(text: String) {
        if (!hazir) return
        val cumleler = text.split(Regex("(?<=[.?!])\\s+")).filter { it.isNotBlank() }
        cumleler.forEachIndexed { i, c ->
            tts?.speak(
                c,
                if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                null,
                "yasli-tts"
            )
        }
    }

    fun kapat() {
        tts?.shutdown()
        tts = null
        hazir = false
    }
}

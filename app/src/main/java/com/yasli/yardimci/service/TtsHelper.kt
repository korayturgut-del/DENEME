package com.yasli.yardimci.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Türkçe sesli okuma. İlk kurulumda ses paketi kontrolü yapılır (G5'te sihirbaza bağlanır).
 */
class TtsHelper(context: Context) : TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var hazir = false

    override fun onInit(status: Int) {
        hazir = status == TextToSpeech.SUCCESS
        if (hazir) {
            tts.language = Locale("tr", "TR")
        }
    }

    fun konus(text: String) {
        if (hazir) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "yasli-tts")
        }
    }

    fun kapat() {
        tts.shutdown()
    }
}

package com.yasli.yardimci.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.theme.YasliTheme

// Tam ekran ilaç/hatırlatıcı alarm ekranı (FSI bildirimiyle açılır)
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val baslik = intent.getStringExtra("baslik") ?: "İlaç saati"
        Tts.init(this)
        setContent {
            YasliTheme {
                AlarmScreen(baslik = baslik) {
                    Tts.konus("Teşekkürler. Kaydedildi.")
                    finish()
                }
            }
        }
    }
}

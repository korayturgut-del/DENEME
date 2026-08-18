package com.yasli.yardimci.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.data.entity.MedicineLog
import com.yasli.yardimci.service.AlarmScheduler
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.theme.YasliTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Tam ekran ilaç/hatırlatıcı alarm ekranı (FSI bildirimiyle açılır)
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val baslik = intent.getStringExtra("baslik") ?: "İlaç saati"
        val tip = intent.getStringExtra("tip") ?: "hatirlatici"
        val requestCode = intent.getIntExtra("requestCode", -1)
        Tts.init(this)

        setContent {
            YasliTheme {
                AlarmScreen(baslik = baslik) {
                    // W3: ilaç alarmında ALINDIM -> MedicineLog kaydı
                    if (tip == "ilac" && requestCode >= 0) {
                        val ilacId = (requestCode - AlarmScheduler.MEDICINE_TABAN).toLong()
                        if (ilacId >= 0) {
                            CoroutineScope(Dispatchers.IO).launch {
                                AppDatabase.get(this@AlarmActivity)
                                    .medicineLogDao()
                                    .ekle(MedicineLog(ilacId = ilacId, tarihSaat = System.currentTimeMillis()))
                            }
                        }
                    }
                    Tts.konus("Teşekkürler. Kaydedildi.")
                    finish()
                }
            }
        }
    }
}

package com.yasli.yardimci

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.Screen
import com.yasli.yardimci.ui.ekranAdi
import com.yasli.yardimci.ui.screens.AsistanEkrani
import com.yasli.yardimci.ui.screens.CallConfirmScreen
import com.yasli.yardimci.ui.screens.ContactsScreen
import com.yasli.yardimci.ui.screens.HomeScreen
import com.yasli.yardimci.ui.screens.KurulumEkrani
import com.yasli.yardimci.ui.screens.MedicinesScreen
import com.yasli.yardimci.ui.screens.MessagesScreen
import com.yasli.yardimci.ui.screens.RemindersScreen
import com.yasli.yardimci.ui.screens.SettingsScreen
import com.yasli.yardimci.ui.screens.WhatsAppScreen
import com.yasli.yardimci.ui.theme.YasliTheme
import com.yasli.yardimci.util.Prefs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Tts.init(this)

        setContent {
            YasliTheme {
                if (!Prefs.kurulumTamam(this)) {
                    KurulumEkrani(onTamam = { Prefs.setKurulumTamam(this, true) })
                } else {
                    AnaIcerik()
                }
            }
        }
    }

    @Composable
    private fun AnaIcerik() {
        var screen by remember { mutableStateOf(Screen.HOME) }
        // ad -> telefon (arama onayı için bekleyen çağrı)
        var cagri by remember { mutableStateOf<Pair<String, String>?>(null) }

        BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

        fun git(s: Screen) {
            screen = s
            Tts.konus(ekranAdi(s))
        }

        fun ara(ad: String, tel: String) {
            cagri = ad to tel
            Tts.konus("$ad aranacak. Vazgeçmek için kırmızı tuşa basın.")
        }

        fun araHemen(ad: String, tel: String) {
            gercekAra(ad, tel)
        }

        Box(Modifier.fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(onGit = ::git, onAra = ::ara, onAraHemen = ::araHemen)
                Screen.REMINDERS -> RemindersScreen(onGeri = { git(Screen.HOME) })
                Screen.CONTACTS -> ContactsScreen(onGeri = { git(Screen.HOME) }, onAra = ::ara)
                Screen.MESSAGES -> MessagesScreen(onGeri = { git(Screen.HOME) })
                Screen.WHATSAPP -> WhatsAppScreen(onGeri = { git(Screen.HOME) })
                Screen.MEDICINES -> MedicinesScreen(onGeri = { git(Screen.HOME) })
                Screen.SETTINGS -> SettingsScreen(onGeri = { git(Screen.HOME) })
                Screen.ASISTAN -> AsistanEkrani(onGeri = { git(Screen.HOME) })
            }

            cagri?.let { (ad, tel) ->
                CallConfirmScreen(
                    ad = ad,
                    tel = tel,
                    onVazgec = {
                        cagri = null
                        Tts.konus("Arama iptal edildi.")
                    },
                    onAra = {
                        cagri = null
                        gercekAra(ad, tel)
                    }
                )
            }
        }
    }

    private fun gercekAra(ad: String, tel: String) {
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$tel")))
            Tts.konus(if (ad.isBlank()) "Aranıyor." else "$ad aranıyor.")
        } catch (e: Exception) {
            Tts.konus("Arama başlatılamadı. Telefon iznini kontrol edin.")
        }
    }
}

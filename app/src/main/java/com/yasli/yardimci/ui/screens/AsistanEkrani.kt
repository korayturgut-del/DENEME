package com.yasli.yardimci.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yasli.yardimci.service.Asistan
import com.yasli.yardimci.service.SesDinleyici
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.theme.CardWhite
import com.yasli.yardimci.ui.theme.Ink
import com.yasli.yardimci.ui.theme.Mavi
import com.yasli.yardimci.ui.theme.Muted
import com.yasli.yardimci.ui.theme.Paper
import com.yasli.yardimci.ui.theme.Turuncu
import com.yasli.yardimci.ui.theme.Yesil
import kotlinx.coroutines.launch

@Composable
fun AsistanEkrani(onGeri: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var durum by remember { mutableStateOf("Hazır") }
    var sonYanit by remember { mutableStateOf("") }
    var dinliyor by remember { mutableStateOf(false) }
    val dinleyici = remember { mutableStateOf<SesDinleyici?>(null) }

    val micIzin = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    val sttVar = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    var sifre by remember { mutableStateOf(0) }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { sifre++ }

    fun bitir() {
        dinleyici.value?.durdur()
        dinleyici.value = null
        dinliyor = false
    }

    DisposableEffect(Unit) { onDispose { bitir() } }

    fun baslat() {
        if (dinliyor) {
            bitir()
            durum = "Hazır"
            return
        }
        if (!micIzin) {
            Tts.konus("Mikrofon izni gerekli.")
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!sttVar) {
            Tts.konus("Bu telefonda ses tanıma yok.")
            return
        }
        durum = "Dinliyorum"
        dinliyor = true
        dinleyici.value = SesDinleyici(
            context,
            onSonuc = { metin ->
                dinliyor = false
                if (metin.isBlank()) {
                    durum = "Hazır"
                    Tts.konus("Anlayamadım, tekrar eder misiniz?")
                    return@SesDinleyici
                }
                durum = "Düşünüyorum"
                scope.launch {
                    val yanit = try {
                        Asistan.istek(context, metin)
                    } catch (e: Exception) {
                        Asistan.yerelMod(context)
                    }
                    sonYanit = yanit
                    Tts.konusSirali(yanit)
                    durum = "Hazır"
                }
            },
            onHata = { mesaj ->
                dinliyor = false
                durum = "Hazır"
                Tts.konus(mesaj)
            }
        )
        dinleyici.value?.baslat()
    }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onGeri,
                colors = ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = Ink),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(56.dp)
            ) { Text("Geri", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Text(
                "ASİSTAN", style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(68.dp))
        }

        Text(
            "Konuşarak isteyin: örneğin \"son mesajlarımı oku\", \"kızımı ara\", " +
                "\"yarın 9'da ilaç hatırlat\"",
            fontSize = 18.sp, color = Muted
        )

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                onClick = ::baslat,
                shape = CircleShape,
                color = when (durum) {
                    "Hazır" -> Yesil
                    "Dinliyorum" -> Turuncu
                    else -> Turuncu
                },
                modifier = Modifier.width(180.dp).height(180.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "MİKROFON\n${durum}",
                        fontSize = 22.sp, fontWeight = FontWeight.Black,
                        color = Color.White, textAlign = TextAlign.Center
                    )
                }
            }
        }
        Text(
            if (dinliyor) "Tekrar dokununca durur" else "Konuşmak için dokunun",
            fontSize = 18.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        if (sonYanit.isNotBlank()) {
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Son yanıt:", fontSize = 18.sp, color = Muted)
                    Text(sonYanit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
            }
            Button(
                onClick = { Tts.konusSirali(sonYanit) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mavi),
                shape = RoundedCornerShape(16.dp)
            ) { Text("TEKRAR OKU", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

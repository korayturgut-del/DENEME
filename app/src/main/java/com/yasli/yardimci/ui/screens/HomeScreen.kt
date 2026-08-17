package com.yasli.yardimci.ui.screens

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.data.entity.QuickDial
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.Screen
import com.yasli.yardimci.ui.theme.CardWhite
import com.yasli.yardimci.ui.theme.Ink
import com.yasli.yardimci.ui.theme.Kirmizi
import com.yasli.yardimci.ui.theme.Line
import com.yasli.yardimci.ui.theme.Mavi
import com.yasli.yardimci.ui.theme.Muted
import com.yasli.yardimci.ui.theme.Paper
import com.yasli.yardimci.ui.theme.SosRed
import com.yasli.yardimci.ui.theme.Turuncu
import com.yasli.yardimci.ui.theme.Yesil
import com.yasli.yardimci.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(onGit: (Screen) -> Unit, onAra: (String, String) -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val kisiler by db.quickDialDao().hepsi().collectAsState(initial = emptyList())

    // İlk açılışta iki örnek kişi (ayarlardan değiştirilebilir)
    LaunchedEffect(Unit) {
        if (kisiler.isEmpty()) {
            db.quickDialDao().ekle(QuickDial(ad = "Kızım", telefon = "0532 111 22 33", renk = "green"))
            db.quickDialDao().ekle(QuickDial(ad = "Oğlum", telefon = "0533 444 55 66", renk = "blue"))
        }
    }

    val tarih = remember {
        SimpleDateFormat("EEEE d MMMM", Locale("tr", "TR")).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Merhaba", style = MaterialTheme.typography.headlineMedium, color = Ink)
            Text(tarih, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }

        // İki hızlı arama tuşu
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val iki = kisiler.take(2)
            (0 until 2).forEach { i ->
                val k = iki.getOrNull(i)
                val renk = if (k?.renk == "blue") Mavi else Yesil
                val ad = k?.ad ?: "Kişi ${i + 1}"
                val tel = k?.telefon ?: ""
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(210.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { if (tel.isNotBlank()) onAra(ad, tel) }
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = renk
                ) {
                    Column(
                        Modifier.fillMaxSize().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CardWhite,
                            modifier = Modifier.height(64.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(ad.take(1), fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(ad, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text("Ara", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
                    }
                }
            }
        }

        // HATIRLATICILAR — tam satır
        BuyukButon("HATIRLATICILAR", Turuncu) { onGit(Screen.REMINDERS) }

        // 2x2 menü
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButonu("Rehber", Yesil, Modifier.weight(1f)) { onGit(Screen.CONTACTS) }
            MenuButonu("Mesajlar", Mavi, Modifier.weight(1f)) { onGit(Screen.MESSAGES) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButonu("WhatsApp", Yesil, Modifier.weight(1f)) { onGit(Screen.WHATSAPP) }
            MenuButonu("Fotoğraflar", Turuncu, Modifier.weight(1f)) { fotograflariAc(context) }
        }

        // İLAÇLARIM — tam satır
        BuyukButon("İLAÇLARIM", Kirmizi) { onGit(Screen.MEDICINES) }

        // SOS — basılı tut (uzun basma) ile tetiklenir
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        val no = Prefs.sosNo(context)
                        if (no.isBlank()) {
                            Tts.konus("SOS numarası ayarlarda seçilmemiş.")
                        } else {
                            onAra("ACİL SOS", no)
                        }
                    })
                },
            shape = RoundedCornerShape(20.dp),
            color = SosRed
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("SOS", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("Basılı tutun", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp)
            }
        }

        // Ayarlar
        Button(
            onClick = { onGit(Screen.SETTINGS) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = Ink),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Ayarlar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun fotograflariAc(context: android.content.Context) {
    try {
        val i = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        context.startActivity(i)
    } catch (e: Exception) {
        Tts.konus("Fotoğraflar açılamadı.")
    }
}

@Composable
private fun BuyukButon(metin: String, renk: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(84.dp),
        colors = ButtonDefaults.buttonColors(containerColor = renk, contentColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(metin, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MenuButonu(metin: String, renk: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = Ink),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(3.dp, Line)
    ) {
        Text(metin, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

package com.yasli.yardimci.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.data.entity.Medicine
import com.yasli.yardimci.data.entity.MedicineLog
import com.yasli.yardimci.data.entity.NotificationLog
import com.yasli.yardimci.data.entity.Reminder
import com.yasli.yardimci.service.AlarmScheduler
import com.yasli.yardimci.service.SmsSender
import com.yasli.yardimci.service.Tts
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
import com.yasli.yardimci.util.ContactHelper
import com.yasli.yardimci.util.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/* ---------- Ortak bileşenler ---------- */

@Composable
private fun Baslik(title: String, onGeri: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onGeri,
            colors = ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = Ink),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(56.dp)
        ) { Text("Geri", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(68.dp))
    }
}

@Composable
private fun BuyukButon(metin: String, renk: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(84.dp),
        colors = ButtonDefaults.buttonColors(containerColor = renk, contentColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) { Text(metin, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}

/* ---------- HATIRLATICILAR ---------- */

@Composable
fun RemindersScreen(onGeri: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val liste by db.reminderDao().aktif().collectAsState(initial = emptyList())
    var ekleme by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("HATIRLATICILAR", onGeri)
        LazyColumn(Modifier.weight(1f)) {
            items(liste) { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).background(CardWhite, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.baslik, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Saat: ${r.saat}", fontSize = 18.sp, color = Muted)
                    }
                    Button(
                        onClick = { Tts.konus(r.baslik) },
                        colors = ButtonDefaults.buttonColors(containerColor = Mavi)
                    ) { Text("Oku", fontSize = 18.sp) }
                }
            }
        }
        if (ekleme) {
            var baslik by remember { mutableStateOf("") }
            var saat by remember { mutableStateOf("09:00") }
            OutlinedTextField(baslik, { baslik = it }, label = { Text("Ne hatırlatayım?") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            OutlinedTextField(saat, { saat = it }, label = { Text("Saat (HH:mm)") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            BuyukButon("KAYDET", Turuncu) {
                scope.launch {
                    val id = db.reminderDao().ekle(Reminder(baslik = baslik.ifBlank { "Hatırlatıcı" }, saat = saat))
                    AlarmScheduler.planla(context, 10000 + id, baslik.ifBlank { "Hatırlatıcı" }, saat)
                    Tts.konus("Hatırlatıcı kaydedildi.")
                    ekleme = false
                }
            }
        } else {
            BuyukButon("+ YENİ HATIRLATICI", Turuncu) { ekleme = true }
        }
    }
}

/* ---------- REHBER ---------- */

@Composable
fun ContactsScreen(onGeri: () -> Unit, onAra: (String, String) -> Unit) {
    val context = LocalContext.current
    val kisiler = remember { ContactHelper.oku(context) }

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("REHBER", onGeri)
        if (kisiler.isEmpty()) {
            Text("Rehbere erişim yok veya rehber boş. İzin verilmiş olmalı.", fontSize = 20.sp, color = Muted)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(kisiler) { (ad, no) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).background(CardWhite, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(ad, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(no, fontSize = 18.sp, color = Muted)
                    }
                    Button(
                        onClick = { onAra(ad, no) },
                        colors = ButtonDefaults.buttonColors(containerColor = Yesil)
                    ) { Text("Ara", fontSize = 18.sp) }
                }
            }
        }
    }
}

/* ---------- MESAJLAR (SMS) ---------- */

@Composable
fun MessagesScreen(onGeri: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val mesajlar by db.notificationLogDao().son("sms").collectAsState(initial = emptyList())
    var yazma by remember { mutableStateOf(false) }
    var secilenKisi by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("MESAJLAR", onGeri)
        if (!yazma) {
            LazyColumn(Modifier.weight(1f)) {
                items(mesajlar) { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp).background(CardWhite, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(m.kimden, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("\"${m.metin}\"", fontSize = 20.sp, color = Muted)
                        }
                        Button(onClick = { Tts.konus("${m.kimden} dedi: ${m.metin}") }, colors = ButtonDefaults.buttonColors(containerColor = Mavi)) { Text("Oku", fontSize = 18.sp) }
                    }
                }
            }
            BuyukButon("MESAJ YAZ", Mavi) { yazma = true }
        } else if (secilenKisi == null) {
            Text("Kime yazalım?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            BuyukButon("Kızım — 0532 111 22 33", Yesil) { secilenKisi = "0532 111 22 33" }
            BuyukButon("Oğlum — 0533 444 55 66", Mavi) { secilenKisi = "0533 444 55 66" }
            BuyukButon("Vazgeç", Kirmizi) { yazma = false }
        } else {
            Text("Ne yazayım?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            val sablonlar = listOf("İyiyim", "Ara beni", "Eve döndüm")
            sablonlar.forEach { metin ->
                BuyukButon(metin, Yesil) {
                    SmsSender.gonder(secilenKisi!!, metin)
                    Tts.konus("Mesaj gönderildi: $metin")
                    yazma = false
                    secilenKisi = null
                }
            }
            BuyukButon("Vazgeç", Kirmizi) { yazma = false; secilenKisi = null }
        }
    }
}

/* ---------- WHATSAPP ---------- */

@Composable
fun WhatsAppScreen(onGeri: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val mesajlar by db.notificationLogDao().son("whatsapp").collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("WHATSAPP", onGeri)
        Text("Bildirim erişimi: açık olmalı", fontSize = 18.sp, color = Muted)
        LazyColumn(Modifier.weight(1f)) {
            items(mesajlar) { m ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).background(CardWhite, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(m.kimden, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("\"${m.metin}\"", fontSize = 20.sp, color = Muted)
                    }
                    Button(onClick = { Tts.konus("${m.kimden} yazdı: ${m.metin}") }, colors = ButtonDefaults.buttonColors(containerColor = Mavi)) { Text("Oku", fontSize = 18.sp) }
                }
            }
        }
        BuyukButon("HEPSİNİ OKU", Yesil) {
            scope.launch {
                Tts.konus("Mesajlar sırayla okunuyor.")
                mesajlar.forEach { m ->
                    Tts.konus("${m.kimden} yazdı: ${m.metin}")
                    delay(3500)
                }
            }
        }
    }
}

/* ---------- İLAÇLARIM ---------- */

@Composable
fun MedicinesScreen(onGeri: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val ilaclar by db.medicineDao().aktif().collectAsState(initial = emptyList())
    var ekleme by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("İLAÇLARIM", onGeri)
        LazyColumn(Modifier.weight(1f)) {
            items(ilaclar) { ilac ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).background(CardWhite, RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${ilac.ad} — ${ilac.saat}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(ilac.doz, fontSize = 18.sp, color = Muted)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                db.medicineLogDao().ekle(MedicineLog(ilacId = ilac.id, tarihSaat = System.currentTimeMillis()))
                                Tts.konus("Teşekkürler. İlacınızı aldığınız kaydedildi.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Kirmizi)
                    ) { Text("ALINDIM", fontSize = 18.sp) }
                }
            }
        }
        if (ekleme) {
            var ad by remember { mutableStateOf("") }
            var doz by remember { mutableStateOf("1 tablet") }
            var saat by remember { mutableStateOf("08:00") }
            OutlinedTextField(ad, { ad = it }, label = { Text("İlaç adı") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            OutlinedTextField(doz, { doz = it }, label = { Text("Doz") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            OutlinedTextField(saat, { saat = it }, label = { Text("Saat (HH:mm)") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            BuyukButon("KAYDET", Kirmizi) {
                scope.launch {
                    val id = db.medicineDao().ekle(Medicine(ad = ad.ifBlank { "İlaç" }, doz = doz, saat = saat))
                    AlarmScheduler.planla(context, 20000 + id, ad.ifBlank { "İlaç" }, saat)
                    Tts.konus("İlaç kaydedildi.")
                    ekleme = false
                }
            }
        } else {
            BuyukButon("+ İLAÇ EKLE", Kirmizi) { ekleme = true }
        }
    }
}

/* ---------- AYARLAR ---------- */

@Composable
fun SettingsScreen(onGeri: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val kisiler by db.quickDialDao().hepsi().collectAsState(initial = emptyList())

    var k1Ad by remember { mutableStateOf("") }
    var k1No by remember { mutableStateOf("") }
    var k2Ad by remember { mutableStateOf("") }
    var k2No by remember { mutableStateOf("") }
    var sosNo by remember { mutableStateOf(Prefs.sosNo(context)) }
    var ses by remember { mutableStateOf(Prefs.sesAcik(context)) }

    LaunchedEffect(kisiler) {
        val k = kisiler.take(2)
        if (k.isNotEmpty()) { k1Ad = k[0].ad; k1No = k[0].telefon }
        if (k.size > 1) { k2Ad = k[1].ad; k2No = k[1].telefon }
    }

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Baslik("AYARLAR", onGeri)
        Text("Hızlı arama 1", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(k1Ad, { k1Ad = it }, label = { Text("Ad") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
        OutlinedTextField(k1No, { k1No = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
        Text("Hızlı arama 2", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(k2Ad, { k2Ad = it }, label = { Text("Ad") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
        OutlinedTextField(k2No, { k2No = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
        Text("SOS numarası", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(sosNo, { sosNo = it }, label = { Text("Acil numara") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))

        BuyukButon("KAYDET", Yesil) {
            scope.launch {
                val k = kisiler.take(2)
                if (k.isNotEmpty()) db.quickDialDao().guncelle(k[0].copy(ad = k1Ad, telefon = k1No))
                if (k.size > 1) db.quickDialDao().guncelle(k[1].copy(ad = k2Ad, telefon = k2No))
                Prefs.setSosNo(context, sosNo)
                Tts.konus("Ayarlar kaydedildi.")
            }
        }
        BuyukButon(if (ses) "SES: AÇIK" else "SES: KAPALI", if (ses) Yesil else Muted) {
            ses = !ses
            Prefs.setSes(context, ses)
            if (ses) Tts.konus("Sesli okuma açıldı.")
        }
    }
}

/* ---------- ALARM EKRANI ---------- */

@Composable
fun AlarmScreen(baslik: String, onAlindi: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Paper).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("İLAÇ SAATİ", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Kirmizi)
        Spacer(Modifier.height(16.dp))
        Text(baslik, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        BuyukButon("ALINDIM", Yesil, onAlindi)
    }
}

/* ---------- ARAMA ONAYI (3 sn Vazgeç) ---------- */

@Composable
fun CallConfirmScreen(ad: String, tel: String, onVazgec: () -> Unit, onAra: () -> Unit) {
    var kalan by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (kalan > 0) {
            delay(1000)
            kalan--
        }
        onAra()
    }

    Surface(Modifier.fillMaxSize(), color = Paper) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("$kalan", fontSize = 110.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text("$ad aranacak", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
            Text(tel, fontSize = 22.sp, color = Muted)
            Spacer(Modifier.height(40.dp))
            BuyukButon("VAZGEÇ", SosRed, onVazgec)
        }
    }
}

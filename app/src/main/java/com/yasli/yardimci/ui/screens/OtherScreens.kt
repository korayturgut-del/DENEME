package com.yasli.yardimci.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.data.entity.Medicine
import com.yasli.yardimci.data.entity.MedicineLog
import com.yasli.yardimci.data.entity.QuickDial
import com.yasli.yardimci.data.entity.temizMetin
import com.yasli.yardimci.data.entity.Reminder
import com.yasli.yardimci.service.AlarmScheduler
import com.yasli.yardimci.service.SmsSender
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.theme.CardWhite
import com.yasli.yardimci.ui.theme.Ink
import com.yasli.yardimci.ui.theme.Kirmizi
import com.yasli.yardimci.ui.theme.Mavi
import com.yasli.yardimci.ui.theme.Muted
import com.yasli.yardimci.ui.theme.Paper
import com.yasli.yardimci.ui.theme.SosKirmizi
import com.yasli.yardimci.ui.theme.Turuncu
import com.yasli.yardimci.ui.theme.Yesil
import com.yasli.yardimci.util.ContactHelper
import com.yasli.yardimci.util.Izinler
import com.yasli.yardimci.util.Prefs
import kotlinx.coroutines.delay
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

private fun saatGecerli(s: String): Boolean =
    Regex("^([01]?\\d|2[0-3]):[0-5]\\d$").matches(s.trim())

private fun klavyeAyari() = KeyboardOptions(imeAction = ImeAction.Done)

private fun doneAksiyon(klavye: SoftwareKeyboardController?) =
    KeyboardActions(onDone = { klavye?.hide() })

private fun alarmIzinYoksaYonlendir(context: android.content.Context) {
    Tts.konus("Alarm izni yok. Kesin alarm iznini verin.")
    context.startActivity(Izinler.exactAlarmAyariIntent(context))
}

/* ---------- HATIRLATICILAR ---------- */

@Composable
fun RemindersScreen(onGeri: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    val liste by db.reminderDao().aktif().collectAsState(initial = emptyList())
    var ekleme by remember { mutableStateOf(false) }
    val klavye = LocalSoftwareKeyboardController.current

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
                        Text("Saat: ${r.saat} · ${tekrarAdi(r.tekrar)}", fontSize = 18.sp, color = Muted)
                    }
                    Button(
                        onClick = { Tts.konus(r.baslik) },
                        colors = ButtonDefaults.buttonColors(containerColor = Mavi)
                    ) { Text("Oku", fontSize = 18.sp) }
                    Spacer(Modifier.width(8.dp))
                    // F5: silme — alarm iptaliyle birlikte
                    Button(
                        onClick = {
                            scope.launch {
                                db.reminderDao().sil(r.id)
                                AlarmScheduler.iptal(context, (AlarmScheduler.REMINDER_TABAN + r.id).toInt())
                                Tts.konus("Hatırlatıcı silindi.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Kirmizi)
                    ) { Text("SİL", fontSize = 18.sp) }
                }
            }
        }
        if (ekleme) {
            var baslik by remember { mutableStateOf("") }
            var saat by remember { mutableStateOf("09:00") }
            var tekrar by remember { mutableStateOf("bugun") }
            OutlinedTextField(baslik, { baslik = it }, label = { Text("Ne hatırlatayım?") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            OutlinedTextField(saat, { saat = it }, label = { Text("Saat (HH:mm)") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp), keyboardOptions = klavyeAyari(), keyboardActions = doneAksiyon(klavye))
            Text("Tekrar:", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TekrarButonu("Bugün", "bugun", tekrar, Modifier.weight(1f)) { tekrar = it }
                TekrarButonu("Her gün", "hergun", tekrar, Modifier.weight(1f)) { tekrar = it }
                TekrarButonu("Hafta içi", "haftaici", tekrar, Modifier.weight(1f)) { tekrar = it }
            }
            BuyukButon("KAYDET", Turuncu) {
                klavye?.hide()
                if (!saatGecerli(saat)) {
                    Tts.konus("Saat geçersiz. Lütfen 09:00 gibi girin.")
                    return@BuyukButon
                }
                scope.launch {
                    val id = db.reminderDao().ekle(Reminder(baslik = baslik.ifBlank { "Hatırlatıcı" }, saat = saat.trim(), tekrar = tekrar))
                    val ok = AlarmScheduler.planla(context, (AlarmScheduler.REMINDER_TABAN + id).toInt(), baslik.ifBlank { "Hatırlatıcı" }, saat.trim(), tekrar, "hatirlatici")
                    if (!ok) alarmIzinYoksaYonlendir(context) else Tts.konus("Hatırlatıcı kaydedildi.")
                    ekleme = false
                }
            }
        } else {
            BuyukButon("+ YENİ HATIRLATICI", Turuncu) { ekleme = true }
        }
    }
}

private fun tekrarAdi(t: String): String = when (t) {
    "hergun" -> "Her gün"
    "haftaici" -> "Hafta içi"
    else -> "Bugün"
}

@Composable
private fun TekrarButonu(ad: String, deger: String, secili: String, modifier: Modifier = Modifier, onSec: (String) -> Unit) {
    Button(
        onClick = { onSec(deger) },
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secili == deger) Turuncu else CardWhite,
            contentColor = if (secili == deger) Color.White else Ink
        ),
        shape = RoundedCornerShape(14.dp)
    ) { Text(ad, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
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
    val scope = rememberCoroutineScope()
    val mesajlar by db.notificationLogDao().son("sms").collectAsState(initial = emptyList())
    val okunmamisSayisi by db.notificationLogDao().okunmamisSayisi("sms").collectAsState(initial = 0)
    val hizli by db.quickDialDao().hepsi().collectAsState(initial = emptyList())
    var yazma by remember { mutableStateOf(false) }
    var secilenKisi by remember { mutableStateOf<Pair<String, String>?>(null) }

    // F2: gerçek kişiler — telefon rehberi + hızlı arama (sabit numara yok)
    val kisiler = remember { ContactHelper.oku(context) } + hizli.map { it.ad to it.telefon }

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("MESAJLAR", onGeri)
        if (!yazma && okunmamisSayisi > 0) {
            Text("$okunmamisSayisi okunmamış mesaj var", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Turuncu)
        }
        if (!yazma) {
            LazyColumn(Modifier.weight(1f)) {
                items(mesajlar) { m ->
                    val kimden = temizMetin(m.kimden)
                    val metin = temizMetin(m.metin)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            .background(if (m.okundu) CardWhite else Turuncu.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (m.okundu) kimden else "● $kimden", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text("\"${metin.ifBlank { "..." }}\"", fontSize = 20.sp, color = Muted)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    db.notificationLogDao().okunduYap(m.id)
                                    Tts.konus(
                                        if (metin.isBlank()) "Bu mesajın içeriği okunamıyor."
                                        else "$kimden dedi: $metin"
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Mavi)
                        ) { Text("Oku", fontSize = 18.sp) }
                    }
                }
            }
            BuyukButon("MESAJ YAZ", Mavi) { yazma = true }
        } else if (secilenKisi == null) {
            Text("Kime yazalım?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (kisiler.isEmpty()) {
                Text("Rehber boş veya izin yok. Ayarlardan hızlı arama kişisi ekleyebilirsiniz.", fontSize = 18.sp, color = Muted)
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(kisiler) { (ad, no) ->
                        BuyukButon("$ad — $no", Yesil) { secilenKisi = ad to no }
                    }
                }
            }
            BuyukButon("Vazgeç", Kirmizi) { yazma = false }
        } else {
            Text("Ne yazayım?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            val sablonlar = listOf("İyiyim", "Ara beni", "Eve döndüm")
            sablonlar.forEach { metin ->
                BuyukButon(metin, Yesil) {
                    val gitti = SmsSender.gonder(secilenKisi!!.second, metin)
                    Tts.konus(if (gitti) "Mesaj gönderildi: $metin" else "Mesaj gönderilemedi. SMS iznini kontrol edin.")
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
    val okunmamisSayisi by db.notificationLogDao().okunmamisSayisi("whatsapp").collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val erisimVar = Izinler.bildirimErisimiVarMi(context)

    Column(Modifier.fillMaxSize().background(Paper).padding(18.dp)) {
        Baslik("WHATSAPP", onGeri)
        if (!erisimVar) {
            Text("Bildirim erişimi kapalı — mesajlar okunamıyor.", fontSize = 18.sp, color = Kirmizi)
            BuyukButon("BİLDİRİM ERİŞİMİNİ AÇ", Mavi) {
                context.startActivity(Izinler.bildirimErisimiIntent(context))
            }
        } else {
            Text("Bildirim erişimi: açık", fontSize = 18.sp, color = Yesil)
        }
        if (okunmamisSayisi > 0) {
            Text("$okunmamisSayisi okunmamış mesaj var", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Turuncu)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(mesajlar) { m ->
                val kimden = temizMetin(m.kimden)
                val metin = temizMetin(m.metin)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        .background(if (m.okundu) CardWhite else Turuncu.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(if (m.okundu) kimden else "● $kimden", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("\"${metin.ifBlank { "..." }}\"", fontSize = 20.sp, color = Muted)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                db.notificationLogDao().okunduYap(m.id)
                                Tts.konus(
                                    if (metin.isBlank()) "Bu mesajın içeriği okunamıyor."
                                    else "$kimden yazdı: $metin"
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Mavi)
                    ) { Text("Oku", fontSize = 18.sp) }
                }
            }
        }
        BuyukButon("HEPSİNİ OKU", Yesil) {
            scope.launch {
                val okunacaklar = mesajlar
                    .map { temizMetin(it.kimden) to temizMetin(it.metin) }
                    .filter { it.second.isNotBlank() }
                if (okunacaklar.isEmpty()) {
                    Tts.konus("Okunacak mesaj yok.")
                } else {
                    // Kuyrukla okunur: QUEUE_ADD birbirini kesmez
                    Tts.ekle("Mesajlar sırayla okunuyor.")
                    okunacaklar.forEach { (k, m) -> Tts.ekle("$k yazdı: $m.") }
                    mesajlar.forEach { db.notificationLogDao().okunduYap(it.id) }
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
    val klavye = LocalSoftwareKeyboardController.current

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
                        Text("${ilac.doz} · ${tekrarAdi(ilac.gunler)}", fontSize = 18.sp, color = Muted)
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
                    Spacer(Modifier.width(8.dp))
                    // F5: silme — alarm iptaliyle birlikte
                    Button(
                        onClick = {
                            scope.launch {
                                db.medicineDao().sil(ilac.id)
                                AlarmScheduler.iptal(context, (AlarmScheduler.MEDICINE_TABAN + ilac.id).toInt())
                                Tts.konus("İlaç silindi.")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Muted)
                    ) { Text("SİL", fontSize = 18.sp) }
                }
            }
        }
        if (ekleme) {
            var ad by remember { mutableStateOf("") }
            var doz by remember { mutableStateOf("1 tablet") }
            var saat by remember { mutableStateOf("08:00") }
            OutlinedTextField(ad, { ad = it }, label = { Text("İlaç adı") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            OutlinedTextField(doz, { doz = it }, label = { Text("Doz") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp))
            OutlinedTextField(saat, { saat = it }, label = { Text("Saat (HH:mm)") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp), keyboardOptions = klavyeAyari(), keyboardActions = doneAksiyon(klavye))
            BuyukButon("KAYDET", Kirmizi) {
                klavye?.hide()
                if (!saatGecerli(saat)) {
                    Tts.konus("Saat geçersiz. Lütfen 08:00 gibi girin.")
                    return@BuyukButon
                }
                scope.launch {
                    val id = db.medicineDao().ekle(Medicine(ad = ad.ifBlank { "İlaç" }, doz = doz, saat = saat.trim(), gunler = "hergun"))
                    val ok = AlarmScheduler.planla(context, (AlarmScheduler.MEDICINE_TABAN + id).toInt(), ad.ifBlank { "İlaç" }, saat.trim(), "hergun", "ilac")
                    if (!ok) alarmIzinYoksaYonlendir(context) else Tts.konus("İlaç kaydedildi. Her gün hatırlatılacak.")
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

    var sosNo by remember { mutableStateOf(Prefs.sosNo(context)) }
    var ses by remember { mutableStateOf(Prefs.sesAcik(context)) }
    val klavye = LocalSoftwareKeyboardController.current

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).imePadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Baslik("AYARLAR", onGeri)
        Text("Hızlı arama kişileri", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        // W1: tam kişi yönetimi — ekle / düzenle / sil
        kisiler.forEach { k ->
            KisiSatiri(
                k = k,
                onKaydet = { ad, no ->
                    scope.launch {
                        db.quickDialDao().guncelle(k.copy(ad = ad.ifBlank { "Kişi" }, telefon = no))
                        Tts.konus("Kişi kaydedildi.")
                    }
                },
                onSil = {
                    scope.launch {
                        db.quickDialDao().sil(k)
                        Tts.konus("Kişi silindi.")
                    }
                }
            )
        }
        Button(
            onClick = {
                scope.launch {
                    db.quickDialDao().ekle(QuickDial(ad = "Yeni Kişi", telefon = "", renk = "green"))
                    Tts.konus("Yeni kişi eklendi. Adını ve numarasını yazın.")
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mavi),
            shape = RoundedCornerShape(16.dp)
        ) { Text("+ KİŞİ EKLE", fontSize = 20.sp, fontWeight = FontWeight.Bold) }

        Text("SOS numarası", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(sosNo, { sosNo = it }, label = { Text("Acil numara") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp), keyboardOptions = klavyeAyari(), keyboardActions = doneAksiyon(klavye))

        BuyukButon("KAYDET", Yesil) {
            klavye?.hide()
            Prefs.setSosNo(context, sosNo)
            Tts.konus("Ayarlar kaydedildi.")
        }
        BuyukButon(if (ses) "SES: AÇIK" else "SES: KAPALI", if (ses) Yesil else Muted) {
            ses = !ses
            Prefs.setSes(context, ses)
            if (ses) Tts.konus("Sesli okuma açıldı.") else Tts.konus("Sesli okuma kapatıldı.")
        }
        Text(
            "Sürüm: ${com.yasli.yardimci.BuildConfig.VERSION_NAME}",
            fontSize = 16.sp, color = Muted
        )
    }
}

@Composable
private fun KisiSatiri(k: QuickDial, onKaydet: (String, String) -> Unit, onSil: () -> Unit) {
    var ad by remember(k.id) { mutableStateOf(k.ad) }
    var no by remember(k.id) { mutableStateOf(k.telefon) }
    val klavye = LocalSoftwareKeyboardController.current
    Column(
        Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(ad, { ad = it }, label = { Text("Ad") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp), keyboardOptions = klavyeAyari(), keyboardActions = doneAksiyon(klavye))
        OutlinedTextField(no, { no = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth(), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp), keyboardOptions = klavyeAyari(), keyboardActions = doneAksiyon(klavye))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { klavye?.hide(); onKaydet(ad, no) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Mavi)
            ) { Text("KAYDET", fontSize = 18.sp) }
            Button(
                onClick = onSil,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Kirmizi)
            ) { Text("SİL", fontSize = 18.sp) }
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
            BuyukButon("VAZGEÇ", SosKirmizi, onVazgec)
        }
    }
}

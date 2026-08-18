package com.yasli.yardimci.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yasli.yardimci.service.Tts
import com.yasli.yardimci.ui.theme.CardWhite
import com.yasli.yardimci.ui.theme.Ink
import com.yasli.yardimci.ui.theme.Mavi
import com.yasli.yardimci.ui.theme.Muted
import com.yasli.yardimci.ui.theme.Paper
import com.yasli.yardimci.ui.theme.Turuncu
import com.yasli.yardimci.ui.theme.Yesil
import com.yasli.yardimci.util.Izinler
import com.yasli.yardimci.util.Prefs

@Composable
fun KurulumEkrani(onTamam: () -> Unit) {
    val context = LocalContext.current
    var sifre by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { sifre++ }

    // Ayarlar ekranlarından dönünce durumları tazele
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) sifre++
        }
        lifecycleOwner.lifecycle.addObserver(o)
        onDispose { lifecycleOwner.lifecycle.removeObserver(o) }
    }

    val eksik = Izinler.eksik(context)
    val nls = Izinler.bildirimErisimiVarMi(context)
    val exact = Izinler.exactAlarmVerildi(context)
    val fsiGerekli = Build.VERSION.SDK_INT >= 34
    val fsiOk = !fsiGerekli || Prefs.fsiOnaylandi(context)
    val hepsiTamam = eksik.isEmpty() && nls && exact && fsiOk

    Column(
        Modifier.fillMaxSize().background(Paper).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("KURULUM", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(
            "Uygulamanın çalışması için aşağıdaki adımları tamamlayın:",
            fontSize = 20.sp, color = Muted
        )

        KurulumAdimi(1, "Telefon izinleri", eksik.isEmpty()) {
            launcher.launch(eksik.toTypedArray())
        }
        KurulumAdimi(2, "Bildirim erişimi (mesajlar)", nls) {
            context.startActivity(Izinler.bildirimErisimiIntent(context))
        }
        KurulumAdimi(3, "Kesin alarm (ilaç/hatırlatıcı)", exact) {
            if (Build.VERSION.SDK_INT >= 31) {
                context.startActivity(Izinler.exactAlarmAyariIntent(context))
            }
        }
        if (fsiGerekli) {
            Row(
                Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "4", fontSize = 26.sp, fontWeight = FontWeight.Black,
                    color = if (fsiOk) Yesil else Turuncu, modifier = Modifier.padding(end = 12.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("Tam ekran bildirimler", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        if (fsiOk) "Tamamlandı" else "Alarmın tam ekran açılması için gerekli",
                        fontSize = 16.sp, color = if (fsiOk) Yesil else Muted
                    )
                }
                if (!fsiOk) {
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = { context.startActivity(Izinler.tamEkranAyariIntent(context)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Mavi)
                        ) { Text("Ayarları Aç", fontSize = 14.sp) }
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { Prefs.setFsiOnaylandi(context, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Yesil)
                        ) { Text("Yaptım", fontSize = 14.sp) }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                Prefs.setKurulumTamam(context, true)
                Tts.konus("Kurulum tamamlandı. Hoş geldiniz.")
                onTamam()
            },
            enabled = hepsiTamam,
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Yesil, contentColor = CardWhite),
            shape = RoundedCornerShape(20.dp)
        ) { Text("DEVAM", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        Text("Devam için tüm adımlar yeşil olmalı.", fontSize = 16.sp, color = Muted)
    }
}

@Composable
private fun KurulumAdimi(no: Int, ad: String, tamam: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$no", fontSize = 26.sp, fontWeight = FontWeight.Black,
            color = if (tamam) Yesil else Turuncu, modifier = Modifier.padding(end = 12.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(ad, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                if (tamam) "Tamamlandı" else "Henüz izin verilmedi",
                fontSize = 16.sp, color = if (tamam) Yesil else Muted
            )
        }
        if (!tamam) {
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Mavi)
            ) { Text("İzin Ver", fontSize = 16.sp) }
        }
    }
}

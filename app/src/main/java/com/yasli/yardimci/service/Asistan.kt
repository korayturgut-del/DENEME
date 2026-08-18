package com.yasli.yardimci.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.yasli.yardimci.data.AppDatabase
import com.yasli.yardimci.data.entity.Medicine
import com.yasli.yardimci.data.entity.Reminder
import com.yasli.yardimci.util.ContactHelper
import com.yasli.yardimci.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/**
 * Sesli asistan çekirdeği: bağlam prefetch → DeepSeek (tek tur) → işlem yürütme.
 * Kurallar: W5 (işlem önce, onay sonra), W6 (hedef yalnız yerel rehber),
 * F12 (yerel mod), F14 (izin kontrolü).
 */
object Asistan {

    suspend fun istek(context: Context, kullaniciMetni: String): String = withContext(Dispatchers.IO) {
        val baglam = baglamMetni(context)
        val istem = "BAĞLAM:\n$baglam\n\nKULLANICI DEDİ: $kullaniciMetni"
        val yanit = DeepseekClient.yanitla(istem)
        when {
            yanit.islem != null -> islemYurut(context, yanit.islem) // W5
            yanit.metin.isNotBlank() -> yanit.metin
            else -> yerelMod(context) // F12: ağ yok/API hatası
        }
    }

    suspend fun yerelMod(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val wa = db.notificationLogDao().son("whatsapp").first().take(3)
        val sms = db.notificationLogDao().son("sms").first().take(3)
        if (wa.isEmpty() && sms.isEmpty()) {
            "İnternet yok ve kayıtlı mesaj bulunamadı."
        } else {
            buildString {
                append("İnternet yok. Son mesajlarınız: ")
                wa.forEach { append("${it.kimden} yazdı: ${it.metin}. ") }
                sms.forEach { append("${it.kimden}: ${it.metin}. ") }
            }
        }
    }

    private suspend fun baglamMetni(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(context)
        val wa = db.notificationLogDao().son("whatsapp").first().take(5)
        val sms = db.notificationLogDao().son("sms").first().take(5)
        val hatirlaticilar = db.reminderDao().aktif().first()
        val ilaclar = db.medicineDao().aktif().first()
        val kisiler = db.quickDialDao().hepsi().first()
        val sos = Prefs.sosNo(context)
        buildString {
            appendLine("HIZLI KİŞİLER:")
            kisiler.forEach { appendLine("- ${it.ad}: ${it.telefon}") }
            if (sos.isNotBlank()) appendLine("SOS numarası: $sos")
            appendLine("SON WHATSAPP MESAJLARI:")
            wa.forEach { appendLine("- ${it.kimden}: ${it.metin}") }
            appendLine("SON SMS:")
            sms.forEach { appendLine("- ${it.kimden}: ${it.metin}") }
            appendLine("AKTİF HATIRLATICILAR:")
            hatirlaticilar.forEach { appendLine("- ${it.baslik} @ ${it.saat} (${it.tekrar})") }
            appendLine("AKTİF İLAÇLAR:")
            ilaclar.forEach { appendLine("- ${it.ad} ${it.doz} @ ${it.saat}") }
        }
    }

    private suspend fun islemYurut(context: Context, islem: JSONObject): String = withContext(Dispatchers.IO) {
        val aksiyon = islem.optString("aksiyon")
        when (aksiyon) {
            "ara" -> {
                val kisi = kisiyiBul(context, islem.optString("hedef"))
                    ?: return@withContext "Kişiyi bulamadım. Ayarlardan kişi ekleyebilirsiniz."
                if (!izinVarMi(context, Manifest.permission.CALL_PHONE)) {
                    return@withContext "Arama için telefon izni gerekli."
                }
                val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:${kisi.second}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i)
                "${kisi.first} aranıyor."
            }
            "sms" -> {
                val kisi = kisiyiBul(context, islem.optString("hedef"))
                    ?: return@withContext "Kişiyi bulamadım."
                if (!izinVarMi(context, Manifest.permission.SEND_SMS)) {
                    return@withContext "SMS izni gerekli."
                }
                val metin = islem.optString("metin").ifBlank { "Selamlar" }
                if (SmsSender.gonder(kisi.second, metin)) {
                    "${kisi.first} kişisine mesaj gönderildi."
                } else {
                    "Mesaj gönderilemedi."
                }
            }
            "hatirlatici_ekle" -> {
                val baslik = islem.optString("metin").ifBlank { "Hatırlatıcı" }
                val saat = islem.optString("saat").ifBlank { "09:00" }
                val tekrar = islem.optString("tekrar").ifBlank { "bugun" }
                val db = AppDatabase.get(context)
                val id = db.reminderDao().ekle(Reminder(baslik = baslik, saat = saat, tekrar = tekrar))
                val ok = AlarmScheduler.planla(context, (AlarmScheduler.REMINDER_TABAN + id).toInt(), baslik, saat, tekrar, "hatirlatici")
                if (!ok) "Alarm izni gerekli. Ayarlardan kesin alarm iznini verin." else "Hatırlatıcı kaydedildi."
            }
            "ilac_ekle" -> {
                val ad = islem.optString("metin").ifBlank { islem.optString("hedef").ifBlank { "İlaç" } }
                val saat = islem.optString("saat").ifBlank { "08:00" }
                val doz = islem.optString("doz").ifBlank { "1 tablet" }
                val db = AppDatabase.get(context)
                val id = db.medicineDao().ekle(Medicine(ad = ad, doz = doz, saat = saat, gunler = "hergun"))
                val ok = AlarmScheduler.planla(context, (AlarmScheduler.MEDICINE_TABAN + id).toInt(), ad, saat, "hergun", "ilac")
                if (!ok) "Alarm izni gerekli. Ayarlardan kesin alarm iznini verin." else "İlaç kaydedildi. Her gün hatırlatılacak."
            }
            "ses_ac" -> {
                Prefs.setSes(context, true)
                "Sesli okuma açıldı."
            }
            "ses_kapat" -> {
                Prefs.setSes(context, false)
                "Sesli okuma kapatıldı."
            }
            else -> "Bu isteği anlayamadım."
        }
    }

    // W6: hedef yalnız yerel kaynaklarda aranır — LLM'in uydurduğu numara asla aranmaz
    private suspend fun kisiyiBul(context: Context, hedef: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            val h = hedef.lowercase(Locale("tr", "TR"))
            if (h.isBlank()) return@withContext null
            val db = AppDatabase.get(context)
            val kisiler = db.quickDialDao().hepsi().first()
            kisiler.firstOrNull {
                val a = it.ad.lowercase(Locale("tr", "TR"))
                a.contains(h) || h.contains(a)
            }?.let { return@withContext it.ad to it.telefon }
            ContactHelper.oku(context).firstOrNull {
                it.first.lowercase(Locale("tr", "TR")).contains(h)
            }
        }

    private fun izinVarMi(context: Context, izin: String): Boolean =
        ContextCompat.checkSelfPermission(context, izin) == PackageManager.PERMISSION_GRANTED
}

/**
 * SpeechRecognizer sarmalayıcısı — tr-TR, yavaş konuşma için 3 sn sessizlik (W7).
 */
class SesDinleyici(
    private val context: Context,
    private val onSonuc: (String) -> Unit,
    private val onHata: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null

    fun baslat() {
        durdur()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val metin = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    onSonuc(metin)
                }

                override fun onError(error: Int) {
                    onHata(hataMesaji(error))
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val ni = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
            sr.startListening(ni)
        }
    }

    fun durdur() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun hataMesaji(kod: Int): String = when (kod) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Anlayamadım, tekrar eder misiniz?"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ses algılanamadı, tekrar deneyin."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "İnternet bağlantısı yok."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni gerekli."
        else -> "Ses algılanamadı, tekrar deneyin."
    }
}

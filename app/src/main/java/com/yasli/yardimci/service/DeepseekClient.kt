package com.yasli.yardimci.service

import com.yasli.yardimci.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek istemcisi — OpenAI uyumlu chat/completions, stream, tek tur islem aracı.
 * Hız: flash model, thinking kapalı, max_tokens 250, 15 sn read timeout (R13).
 */
object DeepseekClient {

    private const val MODEL = "deepseek-v4-flash" // R16: tek sabit
    private const val URL = "https://api.deepseek.com/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class Yanit(val metin: String, val islem: JSONObject?)

    private val SYSTEM_PROMPT =
        "Sen yaşlı bir kullanıcının Türkçe sesli yardımcısısın. Kısa, nazik ve net konuş; " +
            "tek cümle tercih et. İşlem gerektiren isteklerde islem aracını kullan " +
            "(aksiyon: ara, sms, mesaj_oku, hatirlatici_ekle, ilac_ekle, ses_ac, ses_kapat). " +
            "İşlem gerektirmiyorsa soruyu doğrudan yanıtla."

    suspend fun yanitla(istem: String): Yanit = withContext(Dispatchers.IO) {
        val anahtar = BuildConfig.DEEPSEEK_API_KEY
        if (anahtar.isBlank()) return@withContext Yanit("Asistan ayarlanmadı. Geliştiriciyle görüşün.", null) // F15

        val govde = JSONObject().apply {
            put("model", MODEL)
            put("stream", true)
            put("max_tokens", 250)
            put("temperature", 0.3)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", SYSTEM_PROMPT) })
                put(JSONObject().apply { put("role", "user"); put("content", istem) })
            })
            put("tools", JSONArray().apply { put(islemAraci()) })
            put("tool_choice", "auto")
        }

        val req = Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer $anahtar")
            .post(govde.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(req).execute().use { yanit: Response ->
                if (!yanit.isSuccessful) {
                    return@withContext when (yanit.code) {
                        401 -> Yanit("Asistan ayarlanmadı.", null)
                        else -> Yanit("Hizmete ulaşılamadı. Kısa süre sonra tekrar deneyin.", null)
                    }
                }
                val kaynak = yanit.body?.source()
                    ?: return@withContext Yanit("Yanıt alınamadı.", null)
                val metinSb = StringBuilder()
                val argSb = StringBuilder()
                while (!kaynak.exhausted()) {
                    val satir = kaynak.readUtf8Line() ?: break
                    if (!satir.startsWith("data:")) continue
                    val veri = satir.removePrefix("data:").trim()
                    if (veri == "[DONE]") break
                    val obj = JSONObject(veri)
                    val delta = obj.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta") ?: continue
                    delta.optString("content")?.takeIf { it.isNotEmpty() }?.let { metinSb.append(it) }
                    // D5: tool_calls argümanları parçalı gelir — birleştir
                    val tc = delta.optJSONArray("tool_calls") ?: continue
                    for (i in 0 until tc.length()) {
                        tc.optJSONObject(i)?.optJSONObject("function")?.optString("arguments")
                            ?.takeIf { it.isNotEmpty() }?.let { argSb.append(it) }
                    }
                }
                val islem = try {
                    if (argSb.isNotEmpty()) JSONObject(argSb.toString()) else null
                } catch (e: Exception) {
                    null
                }
                Yanit(metinSb.toString(), islem)
            }
        } catch (e: IOException) {
            Yanit("", null) // ağ yok → Asistan yerel moda düşer (F12)
        } catch (e: Exception) {
            Yanit("", null)
        }
    }

    private fun islemAraci(): JSONObject = JSONObject().apply {
        put("type", "function")
        put("function", JSONObject().apply {
            put("name", "islem")
            put("description", "Kullanıcının isteği bir işlem gerektiriyorsa doldur.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("aksiyon", JSONObject().apply {
                        put("type", "string")
                        put("enum", JSONArray(listOf("ara", "sms", "mesaj_oku", "hatirlatici_ekle", "ilac_ekle", "ses_ac", "ses_kapat")))
                    })
                    put("hedef", JSONObject().apply {
                        put("type", "string")
                        put("description", "Kişi adı (hızlı arama veya rehberdeki ad)")
                    })
                    put("metin", JSONObject().apply {
                        put("type", "string")
                        put("description", "SMS içeriği veya hatırlatıcı/ilaç adı")
                    })
                    put("saat", JSONObject().apply {
                        put("type", "string")
                        put("description", "HH:mm biçiminde saat")
                    })
                    put("tekrar", JSONObject().apply {
                        put("type", "string")
                        put("enum", JSONArray(listOf("bugun", "hergun", "haftaici")))
                    })
                    put("doz", JSONObject().apply { put("type", "string") })
                })
                put("required", JSONArray(listOf("aksiyon")))
            })
        })
    }
}

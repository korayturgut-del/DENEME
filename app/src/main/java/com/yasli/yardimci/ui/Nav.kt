package com.yasli.yardimci.ui

enum class Screen { HOME, REMINDERS, CONTACTS, MESSAGES, WHATSAPP, MEDICINES, SETTINGS }

fun ekranAdi(s: Screen): String = when (s) {
    Screen.HOME -> "Ana ekran."
    Screen.REMINDERS -> "Hatırlatıcılar."
    Screen.CONTACTS -> "Rehber. Bir kişiye dokunun, telefon onu arasın."
    Screen.MESSAGES -> "Mesajlar."
    Screen.WHATSAPP -> "WhatsApp mesajları."
    Screen.MEDICINES -> "İlaçlarım."
    Screen.SETTINGS -> "Ayarlar."
}

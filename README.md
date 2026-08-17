# Yardımcım — Yaşlılar için Yardımcı Android Uygulaması

Gözü az gören, unutkan yaşlı kullanıcılar için **büyük butonlu, Türkçe sesli geri bildirimli** Android uygulaması.

## Özellikler

- Ana ekranda **2 büyük hızlı arama tuşu** + tam satır **HATIRLATICILAR** tuşu + **SOS**
- **3 saniye "VAZGEÇ" sayacı** — yanlışlıkla aramayı önler
- **Rehber** (dokununca arar), **Mesajlar** (hazır şablonlarla SMS gönderme)
- **WhatsApp mesajlarını sesli okuma** (bildirim üzerinden; sesli + büyük yazı)
- **Fotoğraflar** kısayolu (galeri açılır)
- **İlaçlarım** (liste + saat alarmı + ALINDIM takibi)
- Türkçe sesli okuma (TTS) — her ekran açılışında ve işlemde geri bildirim

## Teknik

- Kotlin + Jetpack Compose · minSdk 26 (Android 8.0+) · Room DB
- İlaç/hatırlatıcı alarmları `setAlarmClock` ile kurulur (Android 12+/14+ kısıtlarından muaf)

## Gereken izinler

Arama (CALL_PHONE) · Rehber (READ_CONTACTS) · Bildirim (POST_NOTIFICATIONS) · **Bildirim erişimi** (WhatsApp/SMS okuma için) · Batarya optimizasyonu muafiyeti (bildirim dinleyicinin yaşaması için).

## Derleme — iki yol

### A) GitHub Actions (bilgisayarına Android Studio kurmadan) — önerilen
Adım adım: **GITHUB_REHBERI.md**

### B) Android Studio ile
Projeyi aç → Build → Generate Signed APK (detay: onaylı kurulum planı, Faz 9).

## Bilinen sınırlar (v1)

- SMS tam geçmişi okunamaz (Android kısıtı) — mesajlar **bildirim üzerinden** yakalanır.
- İlk sürüm **debug APK** olarak üretilir (yan yükleme); Play Store yayını ayrı bir iş (release imzası gerekir).
- İlk kurulum sihirbazı (izin ekranları) henüz eklenmedi — izinler Android tarafından ilk kullanımda istenir.

# GitHub ile APK Üretme Rehberi (Android Studio kurmadan)

Bu rehber, telefonuna yükleyebileceğin **APK dosyasını GitHub bulutunda üretmeni** sağlar. Kendi bilgisayarına hiçbir program kurmana gerek yok.

## Özet akış

1. GitHub hesabı aç (ücretsiz) → `github.com`
2. Yeni bir **depo** (repository) oluştur (adı örn. `yasli-yardimci`)
3. Bu klasördeki (zip içindeki) tüm dosyaları depoya yükle
4. GitHub otomatik olarak uygulamayı derler (Actions)
5. Çıkan **APK dosyasını indir** ve telefona kur

---

## Adım 1 — GitHub hesabı ve depo

- `github.com`'a gir, **Sign up** ile ücretsiz hesap aç (e-posta yeterli).
- Sağ üstte **+** → **New repository**.
- Repository name: `yasli-yardimci` · **Public** seç (ücretsiz ve sınırsız derleme dakikası).
- **Create repository** de.

## Adım 2 — Dosyaları yükle

Depo sayfası açıkken:

1. **uploading an existing file** (veya "Add file" → "Upload files") bağlantısına tıkla.
2. Bilgisayarında zip'i aç (içindeki `uygulama` klasörü açılır).
3. `uygulama` klasörünün **içindekileri** (klasörü değil, içindekileri) sürükle-bırak. Yani:
   - `app` klasörü
   - `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
   - `.github` klasörü (gizli — görünmüyorsa dosya gezgininde "gizli dosyaları göster" aç)
4. **Commit changes** ile yükle.

> Not: `.github/workflows/build-apk.yml` dosyası mutlaka yüklenmeli — derlemeyi otomatik başlatan o.

## Adım 3 — Derlemenin çalışması

- Dosyalar yüklenince GitHub **otomatik** derlemeye başlar.
- Üstteki **Actions** sekmesine tıkla → "Build APK" işini görürsün (sarı = çalışıyor, yeşil = başarılı, kırmızı = hata).

## Adım 4 — APK'yı indir

- Actions sekmesinde **başarılı (yeşil)** işe tıkla.
- Sayfanın altında **Artifacts** bölümünde `yasli-yardimci-debug` görünür → üstüne tıkla, ZIP indirilir.
- O ZIP'i aç → içinde `app-debug.apk` var.

## Adım 5 — Telefona kur

1. APK'yı telefona aktar (e-posta, kablo, WhatsApp vb.).
2. Telefonda **Ayarlar → Güvenlik → "Bilinmeyen kaynaklara izin ver"** aç (dosya yöneticisi için).
3. APK'ya dokun → **Kur**.

## Adım 6 — Uygulamada açılması gerekenler (kritik)

- İlk açılışta **izinlere izin ver** (arama, rehber, bildirim).
- **Ayarlar → Bildirim erişimi → "Yardımcım"ı aç** (WhatsApp/SMS sesli okuma için).
- **WhatsApp → Ayarlar → Bildirimler → önizlemeyi açık tut** (mesaj metni okunabilsin).
- İlk ilaç/hatırlatıcı kaydında alarm kurulur; **telefonu yeniden başlatınca da alarmlar korunur**.

## Sorun giderme

| Sorun | Çözüm |
|---|---|
| Actions'ta kırmızı (hata) | Hatanın olduğu adıma tıkla, kırmızı log'u bana ilet — düzeltirim |
| APK kurulmuyor | "Bilinmeyen kaynaklara izin" açık mı kontrol et |
| Mesajlar sesli okunmuyor | Bildirim erişimi + WhatsApp önizleme ayarlarını kontrol et |
| Ses hiç yok | Telefonda Türkçe TTS sesi yüklü olmalı (Google TTS otomatik indirir) |

## Android Studio ile derlemek istersen (alternatif)

Android Studio'yu kur → bu klasörü "Open" → Build → Generate Signed APK.

# Apteka 360 — Android ilovasi (native)

Apteka 360 dorixona tarmog‘i boshqaruv tizimining **native Android ilovasi**: Kotlin + Jetpack Compose (Material 3),
to‘g‘ridan-to‘g‘ri API bilan ishlaydi (`https://api.apteka360.uz`, qo‘llanma: `apteka360-backend/docs/API-MOBILE-DESKTOP.md`).
WebView yo‘q — barcha ekranlar native.

| | |
|---|---|
| Ilova nomi | Apteka 360 |
| applicationId | `uz.apteka360.app` (debug: `uz.apteka360.app.debug`) |
| Versiya | 2.0.1 (versionCode 5) — `android/app/build.gradle` |
| minSdk / target | 24 (Android 7.0) / 34 (Android 14) |
| API | `BuildConfig.API_BASE_URL` — `android/app/build.gradle` |
| Stek | Kotlin 2.0, Compose BOM 2024.09, Navigation, OkHttp, kotlinx.serialization, Media3 (HLS + audio), AudioRecord (push-to-talk), EncryptedSharedPreferences, Firebase Messaging |

## Ekranlar

Desktop ilovadagi **barcha 36 bo‘lim** Android’da native (Jetpack Compose) — to‘liq jadval: [COVERAGE-ANDROID.md](COVERAGE-ANDROID.md).
Ko‘rinish `/v1/me` dagi rolga bog‘liq (`ui/nav/Access.kt` — desktop `ACCESS` jadvali bilan bir xil); AUDITOR uchun yozish tugmalari o‘chiq.

- **Navigatsiya:** pastki panelda rolga qarab 4 asosiy bo‘lim + «Yana» (qolgan bo‘limlar guruhlangan: Operatsiyalar, HR, Firmalar,
  Vrachlar, AI nazorat, Analitika, Boshqaruv, Tizim). Planshetda (≥ 600 dp) yon panel (NavigationRail).
  Masalan Egasi: Asosiy · Tasdiqlar · Kameralar · Xabarlar · Yana; HR: Xodimlar · Grafik · Tasdiqlar · Xabarlar · Yana; xodim: Mening ishim · Xabarlar · Profil.
- **Asosiy:** Bosh sahifa (KPI, ogohlantirishlar, filiallar, tasdiqlar, AI, pul zaxirasi, FAB «Tez harakat»), Mening ishim (xodim/mudir mobil ekrani).
- **Operatsiyalar:** Filiallar (+ tafsilot), Sroklar, Kassa yopilishi (FOM farqi, 409 ziddiyat), Harajatlar, Pul oqimi.
- **HR:** Xodimlar (+ forma, bo‘shatish/tiklash, 7 tabli tafsilot, maxfiy oylik — audit bilan), Grafik, Davomat, Jonli filial, Oylik vedomosti, Oylik formula.
- **Firmalar/Vrachlar:** Firmalar va to‘lov kalkulyatori, To‘lov kalendari, Vrachlar va ROI.
- **AI nazorat:** Review navbati, Video devor, Kameralar va zonalar (qo‘shish, sinov, ONVIF qidirish, maxfiylik), jonli ko‘rish
  (to‘liq ekran landshaft, ovoz, bosib turib gapirish).
- **Analitika:** P&L, Analitika, Prognoz, Hisobotlar (PDF/XLSX), Soliq.
- **Boshqaruv/Tizim:** Tasdiqlar, Bildirishlar, Audit, FOM integratsiya, Tizim holati, Foydalanuvchilar (+ Telegram ulash), Rollar,
  Modullar, AI sozlamalari, Sozlamalar, Profil.

**Telefonga xos:** biometrik tasdiq (tasdiqlar; 10 mln+ summalar uchun majburiy), deep link `apteka360://<bo‘lim>/<id>` va push’dan
ekranga o‘tish, FAB, haptic, predictive back, skeleton/pull-to-refresh, yorug‘/qorong‘i/tizim mavzusi, aylantirishda holat saqlanadi.

## API qoidalari (ilova bajaradi)

- Har so‘rovda `X-Client: mobile/<versiya>` va `X-Client-Device: <brend> <model> - Apteka 360`.
- `Authorization: Bearer`; 401 → bitta `refresh` (navbat bilan, bir vaqtda ikkita refresh bo‘lmaydi) → qayta urinish → yana 401 bo‘lsa login.
- Har bir buyruqqa `Idempotency-Key` (UUID).
- Xato matni (`error`) foydalanuvchiga aynan ko‘rsatiladi; `module_disabled` — «modul o‘chirilgan» holati.
- `cameras.streamUrl` havolasi 5 daqiqa — pleer uni 4 daqiqada qayta so‘raydi; `cameras.talk` havolasi ham 4 daqiqa keshlanadi.

Kod: [android/app/src/main/java/uz/apteka360/app](android/app/src/main/java/uz/apteka360/app) —
`data/` (ApiClient, TokenStore, Repository, Models), `ui/auth`, `ui/home`, `ui/notifications`, `ui/cameras`, `ui/profile`, `ui/theme`, `push/`.

## Talablar (build)

- JDK 17 (JDK 25 bilan AGP 8.2 ishlamaydi)
- Android SDK: `platforms;android-34`, `build-tools;34.0.0`, `platform-tools`

Bu kompyuterda toolchain sudo’siz, foydalanuvchi papkalarida ([env.sh](env.sh)):

| Nima | Qayerda |
|---|---|
| Temurin JDK 17 | `~/.local/opt/jdk17` |
| Android SDK + emulyator (AVD `apteka360`) | `~/android-sdk` (`android/local.properties` → `sdk.dir`) |
| Node 22 (faqat lokal backend uchun) | `~/.local/opt/node22` |

```bash
source env.sh
```

## Build

```bash
cd android && ./gradlew assembleDebug
```
→ `android/app/build/outputs/apk/debug/app-debug.apk`

```bash
cd android && ./gradlew assembleRelease
```
→ `android/app/build/outputs/apk/release/app-release.apk` (R8 minify, imzolangan)

Tayyor nusxalar `dist/` ga qo‘yiladi (git’ga tushmaydi).

### Lokal API bilan sinash (emulyator)

```bash
cd ../apteka360-backend && npm install && cd apps/api && PORT=4000 SEED_PASSWORD=demo-local-1234 DEV_OTP=123456 npx tsx src/main.ts
```
```bash
cd android && ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:4000
```
Demo hisoblar: `employee@apteka360.uz`, `manager@apteka360.uz`, `owner@apteka360.uz`, `operator@apteka360.uz` (parol — `SEED_PASSWORD`, kod `123456`).
Prod serverdagi demo parolni loyiha egasidan so‘rang.

### Release imzosi (keystore)

`android/keystore.properties` (namuna: [android/keystore.properties.example](android/keystore.properties.example)) va `*.jks` `.gitignore` da — **repoga tushmaydi**.

```bash
cd android && keytool -genkeypair -v -keystore release.jks -storetype PKCS12 -alias apteka360 -keyalg RSA -keysize 2048 -validity 10000
```

> **Muhim.** Hozirgi `android/release.jks` — vaqtinchalik keystore (parol faqat lokal `keystore.properties` da). Play Market yoki doimiy
> tarqatish uchun yangi keystore yarating va zaxiralang: keystore yo‘qolsa ilovani o‘sha imzo bilan yangilab bo‘lmaydi.

## O‘rnatish (telefon)

1. **Noma’lum manbalardan o‘rnatish** ga ruxsat bering.
2. APK’ni yuboring va oching, yoki USB orqali:

```bash
adb install -r dist/app-release.apk
```

Debug (`uz.apteka360.app.debug`) va release (`uz.apteka360.app`) alohida ilova sifatida yonma-yon turadi.

## CI (GitHub Actions)

[.github/workflows/android.yml](.github/workflows/android.yml): har push/PR da debug APK, Secrets bo‘lsa release ham; artefakt `apteka360-apk`.
Secrets: `ANDROID_KEYSTORE_BASE64` (`base64 -w0 android/release.jks`), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` (`apteka360`), `ANDROID_KEY_PASSWORD`.

## Push (FCM) ulash qadamlari

Kod tayyor (`push/PushService.kt`, `POST_NOTIFICATIONS`, kanal, token saqlash), faqat Firebase konfiguratsiyasi yo‘q:

1. Firebase Console → loyiha → Android ilova, package `uz.apteka360.app` (va `uz.apteka360.app.debug`).
2. `google-services.json` → `android/app/google-services.json` (`.gitignore` da).
3. `android/build.gradle` ga `id 'com.google.gms.google-services' version '4.4.2' apply false`, `app/build.gradle` ga `id 'com.google.gms.google-services'` qo‘shing.
4. `PushTokens.onNewToken` da tokenni backendga yuboring (endpoint `API-MOBILE-DESKTOP.md` bo‘yicha kelishiladi); kirishdan keyin
   `FirebaseMessaging.getInstance().token` ni ham yuboring.
5. Android 13+ da ruxsatni so‘rash: `ActivityResultContracts.RequestPermission()` bilan `POST_NOTIFICATIONS`.

## Sinov (emulyator Android 14, Pixel 6, lokal API)

| Holat | Natija |
|---|---|
| Login → OTP → yangi qurilma → sessiya | ✔ (xodim va mudir) |
| Noto‘g‘ri parol (prod API) | ✔ server matni «Login yoki parol noto‘g‘ri» |
| Xodim: grafik, avans so‘rovi yuborish | ✔ so‘rov serverga yozildi, holat «Kutilmoqda» |
| Bildirishnomalar | ✔ xodim so‘rovi mudirga bildirishnoma bo‘lib keldi |
| Mudir: so‘rovni tasdiqlash | ✔ `m.requestDecide` 200, ro‘yxat yangilandi |
| Ilovani qayta ochish | ✔ refresh token bilan sessiya tiklandi |
| Profil, sessiyalar | ✔ |
| Kameralar ro‘yxati | ✔ (lokal seedda modul o‘chiq — pleer server matnini ko‘rsatdi) |
| Release (R8) APK prod API bilan | ✔ |
| Gapirish (soxta Edge bilan) | ✔ `op=start` → 3200 baytlik PCM bo‘laklar (200 ms) → `op=stop`, indikator, mikrofon ruxsati |

### Unit testlar

```bash
cd android && ./gradlew testDebugUnitTest
```
`TalkProtocolTest` (talk URL, bo‘lak hajmi, xato matnlari, maxSec) va `ModelsTest` (0.6.1 `audio`/`TalkUrl` JSON).

## Biometriya va backend

`approvals.decide` da `biometricToken` faqat Telegram Mini App kanali uchun talab qilinadi; mobil sessiya backendda `web` kanali hisoblanadi.
Shuning uchun Android’dagi BiometricPrompt — telefondagi qo‘shimcha himoya (server token so‘ramaydi). Sozlama: Profil → Ilova → Biometrik tasdiq.

## Cheklovlar

- **Kamera ovozi va gapirish haqiqiy Edge/kamera bilan sinalmagan** (emulyatorda mikrofon va Hikvision karnay yo‘q): talk oqimi
  soxta Edge serveri bilan tekshirildi (`tools`siz, `/tmp` skript). Yubileyniy Edge’da `cam-c1`/`cam-c16` audio bor — haqiqiy telefonda
  mudir hisobi bilan sinash kerak. HLS’dagi AAC audio trekni ExoPlayer standart ijro qiladi.
- **HLS jonli oqim** haqiqiy kamera bilan sinalmagan: lokal seedda «Kameralar» moduli o‘chiq, prod uchun demo parol yo‘q.
  Pleer Media3 ExoPlayer HLS — standart; oqim HTTPS bo‘lishi kerak (cleartext faqat `10.0.2.2`/`localhost` uchun debugda ruxsat).
- **Smena almashish**: `/v1/employees` xodimga faqat o‘zini qaytaradi — hamkasb ro‘yxati bo‘lmasa ID qo‘lda kiritiladi (API cheklovi).
- Push — FCM konfiguratsiyasisiz (yuqoridagi qadamlar). Face ID / biometriya yo‘q.
- Release keystore vaqtinchalik. iOS yo‘q.
- Ta’til sanasi matn sifatida kiritiladi (API `label` matn qabul qiladi), kalendar tanlagich yo‘q.

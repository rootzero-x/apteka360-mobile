# Apteka 360 — Android ilovasi

Apteka 360 dorixona tarmog‘i boshqaruv tizimining Android ilovasi. Ilova **Capacitor 6** asosida qurilgan
va WebView ichida web-panelni (`https://apteka.agrom24.uz`) ochadi. Barcha biznes mantiq web-panelda;
bu repo faqat native qobiq, ikonka/splash, offline ekran va build/imzolash sozlamalarini saqlaydi.

| | |
|---|---|
| Ilova nomi | Apteka 360 |
| applicationId | `uz.apteka360.app` |
| Versiya | 1.0.0 (versionCode 1) — `android/app/build.gradle` |
| minSdk / target | 22 (Android 5.1) / 34 (Android 14) |
| Web manzil | `https://apteka.agrom24.uz` — `capacitor.config.json` → `server.url` |
| Orientatsiya | portret va landshaft (cheklanmagan) |

## Nimalar qilingan

- **WebView qobig‘i** — `server.url` orqali prod web-panel yuklanadi; lokal `www/` faqat offline sahifa uchun.
- **Orqaga tugmasi** WebView tarixida orqaga yuradi; tarix tugaganda ilova yopilmaydi, orqa fonga o‘tadi
  ([MainActivity.java](android/app/src/main/java/uz/apteka360/app/MainActivity.java)).
- **Internet yo‘q** — o‘zbekcha xato ekrani ([www/error.html](www/error.html)) va «Qayta urinish» tugmasi
  (Capacitor `server.errorPath`). Tarmoq qaytganda ilova o‘zi bosh sahifani qayta yuklaydi (ConnectivityManager).
- **Tashqi havolalar** (boshqa domen, `tel:`, `mailto:`, Telegram va h.k.) tizim brauzerida/ilovasida ochiladi —
  Capacitor `Bridge.launchIntent` standart xatti-harakati; `server.url` domeni WebView ichida qoladi.
- **X-Client sarlavhasi** — `X-Client: mobile/1.0.0` va `X-Client-Device: <brend> <model> · Apteka 360`
  asosiy (main-frame) sahifa so‘rovlariga qo‘shiladi. Cheklov: WebView ichidagi `fetch`/XHR so‘rovlariga native
  tomondan sarlavha qo‘shib bo‘lmaydi (pastdagi «Cheklovlar»ga qarang).
- **User-Agent** ga `Apteka360Mobile/1.0.0 (android)` qo‘shiladi — web-panel shu orqali mobil ilovani aniqlay oladi.
- **Ikonka va splash** — `assets/` dagi SVG manbalardan `@capacitor/assets` bilan generatsiya qilinadi (adaptive icon, light/dark splash).
- **Push uchun joy** — `@capacitor/push-notifications` o‘rnatilgan, `POST_NOTIFICATIONS` ruxsati manifestda,
  `google-services.json` bo‘lsa Gradle plagin avtomatik ulanadi (pastda qadamlar).
- **HLS video** — Android System WebView (Chromium) `<video src="…m3u8">` ni native qo‘llab-quvvatlaydi;
  qo‘shimcha kutubxona kerak emas. Fullscreen video Capacitor `BridgeWebChromeClient` orqali ishlaydi.

## Talablar (build uchun)

- Node.js 22 + npm
- JDK 17 (Android Gradle Plugin 8.2 uchun; JDK 25 bilan ishlamaydi)
- Android SDK: `platforms;android-34`, `build-tools;34.0.0`, `platform-tools`
  (Android Studio yoki `cmdline-tools` + `sdkmanager`)

Bu kompyuterda toolchain sudo’siz, foydalanuvchi papkalariga o‘rnatilgan va [env.sh](env.sh) orqali ulanadi:

| Nima | Qayerda |
|---|---|
| Node 22 (npm bilan) | `~/.local/opt/node22` |
| Temurin JDK 17 | `~/.local/opt/jdk17` |
| Android SDK | `~/android-sdk` (`android/local.properties` → `sdk.dir`) |

```bash
source env.sh
```

## Build

```bash
npm ci
npx cap sync android
```

Debug APK:

```bash
cd android && ./gradlew assembleDebug
```

→ `android/app/build/outputs/apk/debug/app-debug.apk`

Release APK (imzolangan):

```bash
cd android && ./gradlew assembleRelease
```

→ `android/app/build/outputs/apk/release/app-release.apk`

Yoki qisqa: `npm run build:debug`, `npm run build:release`. Tayyor APK nusxalari `dist/` ga qo‘yiladi (git’ga tushmaydi).

### Release imzosi (keystore)

Release build `android/keystore.properties` faylini o‘qiydi (namuna: [android/keystore.properties.example](android/keystore.properties.example)).
Fayl ham, `*.jks` ham `.gitignore` da — **repoga tushmaydi**.

Yangi keystore yaratish:

```bash
cd android && keytool -genkeypair -v -keystore release.jks -storetype PKCS12 -alias apteka360 -keyalg RSA -keysize 2048 -validity 10000
```

so‘ng `android/keystore.properties` ga `storeFile=release.jks`, parollar va `keyAlias=apteka360` yoziladi.

> **Muhim.** Hozirgi `android/release.jks` — vaqtinchalik keystore (paroli faqat lokal `keystore.properties` da).
> Play Market yoki doimiy tarqatish uchun **yangi, saqlanadigan keystore** yarating va uni ishonchli joyda zaxiralang:
> keystore yo‘qolsa, o‘sha imzo bilan ilovani yangilab bo‘lmaydi (foydalanuvchilar o‘chirib qayta o‘rnatishi kerak bo‘ladi).

## O‘rnatish (telefon)

1. Telefonda **Sozlamalar → Xavfsizlik → Noma’lum manbalardan o‘rnatish** ga ruxsat bering (yoki APK ochilganda so‘raladi).
2. APK’ni telefonga yuboring (Telegram/USB) va oching, yoki USB orqali:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Debug va release APK’lar turli imzo bilan, shuning uchun biri turganda ikkinchisini o‘rnatishdan oldin eskisini o‘chiring.

## CI (GitHub Actions)

[.github/workflows/android.yml](.github/workflows/android.yml) har `push`/PR da debug APK, Secrets bo‘lsa release APK ham quradi
va `apteka360-apk` artefakti sifatida yuklaydi. Release uchun repo Secrets:

| Secret | Qiymat |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 android/release.jks` natijasi |
| `ANDROID_KEYSTORE_PASSWORD` | keystore paroli |
| `ANDROID_KEY_ALIAS` | `apteka360` |
| `ANDROID_KEY_PASSWORD` | kalit paroli |

## Push (FCM) ulash qadamlari

Kod va plagin tayyor, faqat Firebase konfiguratsiyasi yo‘q:

1. [Firebase Console](https://console.firebase.google.com) da loyiha yarating, Android ilova qo‘shing: package `uz.apteka360.app`.
2. `google-services.json` ni yuklab, `android/app/google-services.json` ga qo‘ying (`.gitignore` da — commit qilinmaydi;
   CI uchun base64 Secret sifatida yozib, workflow’da faylga chiqaring).
3. `android/build.gradle` da `com.google.gms:google-services` classpath allaqachon bor; `app/build.gradle` fayl topilsa plaginni o‘zi ulaydi.
4. Web-panel tomonida (Capacitor ichida ekanini `window.Capacitor?.isNativePlatform()` yoki User-Agent orqali aniqlab):
   ```js
   import { PushNotifications } from '@capacitor/push-notifications';
   await PushNotifications.requestPermissions();
   await PushNotifications.register();
   PushNotifications.addListener('registration', t => api.post('/v1/devices/push-token', { token: t.value }));
   ```
   (Backend endpointi — `apteka360-backend/docs/API-MOBILE-DESKTOP.md` bo‘yicha kelishiladi.)
5. Qayta `npx cap sync android` va build.

## Versiyani ko‘tarish

`android/app/build.gradle` → `versionCode` (+1) va `versionName`; `capacitor.config.json` → `appendUserAgent` dagi versiya;
`package.json` → `version`. `MainActivity` X-Client qiymatini `BuildConfig.VERSION_NAME` dan oladi.

## Loyiha tuzilmasi

```
capacitor.config.json   # appId, server.url, errorPath, splash
www/                    # error.html (offline), index.html (zaxira redirect)
assets/                 # ikonka/splash SVG va PNG manbalari (npm run assets)
android/                # native Android loyihasi (Gradle)
  app/src/main/java/uz/apteka360/app/MainActivity.java
  keystore.properties.example
.github/workflows/android.yml
env.sh                  # lokal toolchain PATH
```

## Cheklovlar

- **X-Client XHR’da yo‘q.** Android WebView `fetch`/XMLHttpRequest so‘rovlariga native tomondan sarlavha qo‘sha olmaydi.
  Sarlavha faqat sahifa (main-frame) so‘rovlarida ketadi. API so‘rovlarida `X-Client: mobile/<versiya>` bo‘lishi uchun
  web-panel (`apteka360-frontend-web`) Capacitor ichida ekanini aniqlab (`window.Capacitor?.isNativePlatform()` yoki
  UA’dagi `Apteka360Mobile/<versiya>`) o‘zi sarlavhani qo‘shishi kerak — 3–5 qatorlik o‘zgarish.
- **HLS jonli ko‘rish** qurilmada tekshirilmagan (bu kompyuterda Android qurilma yo‘q). WebView HLS’ni qo‘llaydi,
  lekin kamera oqimi HTTPS bo‘lishi shart (`allowMixedContent=false`); `http://` HLS bo‘lsa, kameralar proxy orqali HTTPS’da berilishi kerak.
- **Offline ekran** faqat asosiy sahifa yuklanmaganda chiqadi. Sahifa ochiq turganda internet uzilsa, web-panelning o‘z xato xabarlari ishlaydi.
- **Sessiya saqlanishi** web-panel cookie/localStorage’iga bog‘liq; WebView ularni ilova o‘chirilguncha saqlaydi.
- **Release keystore vaqtinchalik** (yuqoriga qarang). Push — FCM konfiguratsiyasisiz, faqat joy tayyor.
- Ilova iOS uchun sozlanmagan (`npx cap add ios` macOS’da qo‘shiladi).

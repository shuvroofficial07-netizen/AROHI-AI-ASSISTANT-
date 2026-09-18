# AROHI AI Assistant by Shù Vrô

**Version 14.0.0 — Bengali-first AI companion with a real productivity & pro-active layer**

AROHI is a real, working Android AI assistant built with Kotlin + Jetpack Compose. No fake data, no simulated actions — every reading and every action comes from real device APIs.

![Icon](app/src/main/res/drawable/arohi_app_icon_1788201070726.jpg)

## ✅ What actually works (real implementation)

| Feature | How it works for real |
| --- | --- |
| ☁️ Cloud AI brain | Gemini API (`gemini-3.5-flash` by default) via direct REST — bring your own API key |
| 🎙️ Voice input | Android `SpeechRecognizer` (bn-BD / Bengali, plus other languages) with runtime mic permission request |
| 🔊 Voice output | Android `TextToSpeech` with adjustable pitch/speed, Bengali voice preferred |
| 🔦 Flashlight | Real `CameraManager.setTorchMode` with hardware capability detection |
| 🔋 Battery / RAM / Storage / Network | Real `BatteryManager`, `ActivityManager`, `StatFs`, `ConnectivityManager` readings |
| 🔉 Volume | Real `AudioManager` stream volume set/get |
| 📞 Calls | Real `ACTION_CALL` (with permission) or `ACTION_DIAL` fallback, contact lookup via `ContactsContract` |
| 💬 SMS / WhatsApp | Real `ACTION_SENDTO` intents and WhatsApp deep links |
| 🧠 Memory | Real Room database persistence (`memories` table) |
| 🔔 Notification inbox | Real `NotificationListenerService` capture into Room + real AI summary computed from actual data |
| 📱 Screen control | Real `AccessibilityService`: read screen, click elements, back/home/recents/notifications |
| ⚙️ Routines | Trigger phrases really execute their action lists (telemetry, quiet volume, silence mode, torch, notifications, diagnostics) |
| ✅ Smart Tasks | Saved commands persisted in Room, executed through the real brain, genuine success/failure recorded |
| 🩺 System Health | Real-time checks of permissions, services, camera hardware, battery optimization and Gemini link latency |
| 👁️ Vision | Real CameraX capture → Base64 → Gemini multimodal analysis |
| 🗓️ Reminders & alarms | Real `AlarmManager` alarms (exact where the OS allows), recurring rules (daily / weekly / weekdays / monthly), re-armed after reboot or app update |
| ✅ To-do list | Room-backed tasks, addable by voice or from the Productivity tab, completable by title |
| 📝 Notes | Room-backed notes with search, creatable by voice |
| ⛅ Weather | Real Open-Meteo forecast for your GPS location or any city, cached offline, with rain alerts |
| 🔢 Calculator & converters | Offline expression calculator, unit converter (length/weight/temperature/…) and currency converter (live rates with offline fallback) |
| 🌐 Translation | Bengali ⇄ English quick translation via the cloud brain, with an offline phrase book fallback |
| 🗣️ Emotion-tagged replies | Every reply starts with `<emotion>…</emotion>`, stripped before speaking so the avatar animates with the real emotion |
| 🧑‍🎤 Avatar studio | 2D portrait / 3D orb switch, idle animation, five accent palettes, five outfits, personality slider |
| 🌅 Pro-active check-ins | Optional morning & evening notifications with weather-aware, time-aware suggestions (rain → umbrella) |
| 🛡️ Privacy center | At-rest encryption of chat + memories (Keystore AES-GCM), JSON export/share, delete-all, optional Firestore backup |
| 🧠 Claude option | Optional Anthropic (Claude) brain provider — same persona, same tools, bring your own key |
| ♿ Accessibility | Font-size scale, high-contrast palette, TTS speed/pitch control |

## 📲 Install the APK

1. Go to the [**Releases**](../../releases) page of this repository.
2. Download the latest `AROHI-AI-Assistant-debug-build-*.apk`.
3. On your phone, allow **Install unknown apps** for your browser/file manager, then open the APK.

Every push to `main` automatically builds a fresh APK via GitHub Actions (`.github/workflows/build-apk.yml`). You can also download it from the workflow's **Artifacts** section.

## 🔑 One-time setup inside the app

1. Open **AROHI** → **SETTINGS** tab.
2. Paste your free **Gemini API key** from [https://aistudio.google.com/apikey](https://aistudio.google.com/apikey) and tap **Save & Connect**. AROHI verifies the key with a real API ping.
3. Grant the optional permissions when asked (they enable the corresponding real features):
   - **Microphone** → voice input
   - **Contacts / Phone** → "রাহিম কে কল দাও"
   - **Notification Access** → notification inbox intelligence
   - **Accessibility** → screen reading and device navigation
   - **Notifications** → background operating service

Without a Gemini key AROHI still works offline through its local engines (battery, torch, volume, calls, apps, navigation, routines, reminders, to-dos, notes, calculator, unit/currency conversion, weather cache, Bengali phrase book).

### Optional: use Claude instead of Gemini

**Settings → ব্রেইন প্রোভাইডার → Claude**, paste an Anthropic API key and save. AROHI's personality,
tools, emotion contract and Bengali-first behaviour are identical — only the cloud engine changes.
Both options are free to choose and nothing is locked behind a plan.

### Everything is free

AROHI has **no premium locks**: every feature above is available in the debug APK.

## 🛠️ Build it yourself

```bash
# Requires JDK 17+; Android SDK is downloaded automatically by the Gradle plugin
./gradlew :app:assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

CI builds automatically on every push — see `.github/workflows/build-apk.yml`.

## 📁 Project structure

```
app/src/main/java/com/example/
├── engine/        # ArohiBrain (Gemini/Claude + 24 tools), persona, emotion tags, time phrases,
│                  # productivity router, offline fallback, pro-active suggestions, calculators
├── data/          # Room database (8 tables + migration), repositories, Gemini & Anthropic clients
├── privacy/       # Keystore AES-GCM vault, data export/delete, optional Firestore backup
├── device/        # Real device managers (battery, torch, audio, telephony, contacts, apps),
│                  # alarm scheduler, calendar helper, location helper, weather client
├── service/       # Foreground service, accessibility, notification listener, reminder alarms,
│                  # morning/evening check-ins, boot re-arming, diagnostics, notification helper
├── voice/         # SpeechRecognizer + TextToSpeech managers
└── ui/            # Compose UI (Home, Chat, Device, Inbox, Memory, Vision, Health, Tasks,
                   # Productivity, Weather & check-ins, Avatar Studio, Privacy Center, Settings)
```

---

**AROHI AI Assistant** — by **Shù Vrô (Shuvro)**

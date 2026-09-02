# AROHI AI Assistant by Shù Vrô

**Version 14.0.0 — Autonomous Android AI Operating Layer (Final Production Release)**

AROHI is a real, working Android AI assistant built with Kotlin + Jetpack Compose. No fake data, no simulated actions — every reading and every action comes from real device APIs.

![Icon](app/src/main/res/drawable/arohi_app_icon_1788201070726.jpg)

## 🛠 What was repaired in v14.0.0 (no fake functionality)

- **Startup**: the branded splash ("Arohi AI Assistant by Shù Vrô") is now the real start destination — previously it was registered but never shown; the app opened straight to Home.
- **Vision AI**: removed the hardcoded "Monstera Deliciosa 96%" detection box, the fabricated plant-health card and the always-on LIVE badge. The gallery button now opens the REAL Android photo picker and sends the actual image to Gemini; the LIVE/NO SIGNAL badge reflects the real CameraX bind state; capture errors are reported honestly.
- **Silence command**: "টর্চ বন্ধ করো" / "stop the music" were hijacked by the over-eager silence matcher — now strict phrase matching with device-keyword exclusion, and "চুপ করো" now actually stops TTS instead of announcing "I'll be quiet" out loud.
- **Offline memory**: "মনে রেখো …" really writes to the Room memories table (verified by row id) and "কী মনে আছে?" really reads it back — even without an API key.
- **Network layer**: structured error model (DNS failure / timeout / TLS / HTTP 400-5xx / invalid key / rate limit / model unavailable) with real Bengali+English explanations, retry with backoff on transient errors, and the API key moved from the URL query string to the `x-goog-api-key` header so it can never leak into logs.
- **API key security**: the key is now stored in EncryptedSharedPreferences (Android Keystore) with automatic migration of existing plaintext keys.
- **Background service**: switched from the `microphone` FGS type (which crashes on Android 14+ when mic permission is missing) to the honest `specialUse` type; start failures are caught and reported with the real exception instead of crashing.
- **Speech recognition**: added real availability check, honest STARTING→LISTENING state transitions, and busy-engine recovery; the UI now shows what the recognizer is actually doing.
- **TTS**: text requested during engine init is queued and spoken once ready — the "Test Voice" button can no longer silently do nothing.
- **Diagnostics**: new REAL checks — live network/validated-internet state, a genuine database write→read→delete probe, TTS engine presence, and storage writability — all wired to the existing Health screen with working action buttons.
- **Notification listener**: scoped coroutines + honest disconnect state (no stale "connected").
- **No dead buttons**: the Inbox filter button now genuinely toggles all/unread; the Settings "Test Voice" button reports a real not-ready state.
- Removed the unused `SYSTEM_ALERT_WINDOW` permission (permission minimization).

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

Without a Gemini key AROHI still works offline through its local command engine (battery, torch, volume, calls, apps, navigation, routines).

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
├── engine/        # ArohiBrain (Gemini + tools), LocalCommandEngine, routines, verification
├── data/          # Room database, repositories, Gemini REST client (Retrofit + Moshi)
├── device/        # Real device managers (battery, torch, audio, telephony, contacts, apps)
├── service/       # Foreground service, AccessibilityService, NotificationListenerService, diagnostics
├── voice/         # SpeechRecognizer + TextToSpeech managers
└── ui/            # Compose UI (Home, Chat, Device, Inbox, Memory, Vision, Health, Tasks, Settings)
```

---

**AROHI AI Assistant** — by **Shù Vrô (Shuvro)**

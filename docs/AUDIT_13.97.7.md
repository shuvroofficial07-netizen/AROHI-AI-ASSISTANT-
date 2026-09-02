# AROHI AI ASSISTANT — Stabilization Audit (v13.97.7)

**Baseline (safe restore point):** commit `2f5549d` on `main` — nothing was deleted; every change
in this branch is an additive commit on top of that baseline, so any subsystem can be restored with
`git revert` / `git checkout 2f5549d -- <path>`.

**Working branch:** `arena/01a05ff3-arohi-ai-assistant`
**Build verification:** GitHub Actions (`.github/workflows/build-apk.yml`) — `./gradlew :app:assembleDebug`
on JDK 17 / Gradle 9.3.1, producing an installable debug APK artifact + release.

---

## 1. P0 — "Unfortunately, AROHI has stopped" at launch

**Root cause (confirmed, not guessed):** the app shipped Firebase dependencies
(`firebase-bom`, `firebase-ai`, `firebase-appcheck-recaptcha`, `firebase-appcheck-debug`) and the
`com.google.gms.google-services` plugin, but the repository contains **no `google-services.json`**
and no Kotlin code ever calls Firebase. `FirebaseInitProvider` is merged into the manifest and runs
*before* `Application.onCreate()`; with no `google_app_id` resource it aborts startup
("Default FirebaseApp is not initialized"), which the user sees as an immediate crash.

**Fix:**
* removed the four unused Firebase dependencies and the `google-services` Gradle plugin;
* explicitly stripped the provider from the merged manifest:
  `<provider android:name="com.google.firebase.provider.FirebaseInitProvider" tools:node="remove" />`.

**Secondary startup hardening (no crash suppression):**
* `CrashReporter` installs a default uncaught-exception handler that **persists the real stack trace**
  and then delegates to the platform handler — the process still dies exactly as Android intends;
  the report is surfaced in Diagnostics so a crash on a phone without adb is still diagnosable.
* `ArohiApplication.onCreate()` no longer indexes installed apps on the main thread and cannot fail startup.
* `ArohiViewModel` initial telemetry read is wrapped: a device-state failure yields an
  "Unavailable" snapshot instead of an exception inside the ViewModel constructor.
* Build blocker also fixed: KSP `2.3.5 → 2.3.6` (2.3.5 aborts in headless/CI Gradle workers).

## 2. Build system

| Item | Before | After |
|---|---|---|
| versionName / versionCode | 13.99.0 / 13990 | **13.97.7 / 139707** |
| KSP | 2.3.5 (fails headless) | 2.3.6 |
| Firebase / google-services | present, unused, crashing | removed |
| CI workflow | `build-apk-ci.yml` in repo root (never ran) | `.github/workflows/build-apk.yml` (runs, uploads APK, reports failures) |

`minSdk 24` keeps Android 9 / API 28 (Galaxy S8+) support; all new APIs are version-guarded.

## 3. Removed fake data / fake status

| Location | Fake behaviour | Replacement |
|---|---|---|
| Vision screen | Stock "plant" photo shown as the camera feed when permission was denied; hard-coded "Monstera Deliciosa 96%" detection box; hard-coded plant-care card; permanent green "LIVE" badge | Real CameraX preview only; badge reports `CAMERA ACTIVE` / `STARTING…` / `CAMERA ERROR` / `NOT AVAILABLE` from real bind state; camera errors are shown verbatim; gallery button now opens a real picker and sends the chosen image to Gemini |
| Splash | Progress bar claiming "Checking voice engine / Gemini / permissions / notification intelligence" while only sleeping | Branding animation only; real checks live in Diagnostics |
| Device dashboard & Home | `0`, `50%`, `64%`, `62%` fallbacks when Android returned nothing | `Unavailable` everywhere (battery, RAM, storage, volume, brightness, network, charging type) |
| Brain tool results | `"কমান্ড '$name' সম্পন্ন করা হয়েছে।"` for *any* unknown tool | Reports that no such tool exists and nothing was done |
| Diagnostics | "24/7 assistant capabilities enabled", "uninterrupted background execution" | States the real Android/OEM limits |
| App launch | Always "সফলভাবে চালু করা হয়েছে" if `startActivity` did not throw | Verified against the real foreground package via Accessibility; otherwise explicitly says verification was not possible |
| Version label | "Version 13.99.0 (Final Production Release)" | "Version 13.97.7" |

## 4. Gemini (real configuration)

* Model list is real and selectable (`gemini-2.5-flash/pro`, `gemini-2.0-flash`, `gemini-1.5-flash/pro`);
  the previous default `gemini-3.5-flash` does not exist on the API and is migrated automatically.
* Real connection test (minimal `generateContent` call) that maps HTTP 400/401/403/404/429/5xx to
  distinct honest states: `INVALID KEY`, `MODEL UNAVAILABLE`, `RATE LIMITED`, `NETWORK ERROR`.
* Configurable request timeout (5–180 s) and bounded retry (0–3, exponential backoff, never infinite).
* **Key security:** stored encrypted with an Android Keystore AES/GCM key (API 23+; on older devices
  the UI says plainly that only app-private storage is used), never logged — the OkHttp logger
  redacts `key=…`, and logging is disabled entirely in release builds. Clear-key action included.
* When no key exists the UI states **"Gemini API is not configured."** — local commands still work.

## 5. Background service

* Foreground service type is chosen from real permission state (`microphone` only when RECORD_AUDIO
  is granted, otherwise `dataSync`) — this is what throws `SecurityException` on Android 14+.
* `START_STICKY`, stop action in the notification, real observable state
  (`STOPPED / STARTING / RUNNING / FAILED`) plus the platform's failure message.
* The UI reflects that live state; no claim is made that the service can survive force-stop or an
  OEM battery kill.

## 6. Notification intelligence (real)

* Announcements are driven by a real user policy: `OFF / IMPORTANT ONLY / SELECTED APPS / ALL`.
* Privacy mode announces the sender only. DND, silent/vibrate ringer, an active call and silence
  mode all mute Arohi.
* When the notification carries no readable body Arohi says exactly that — content is never invented.

## 7. Permissions

New **Permission Center** (Settings): microphone, contacts, phone, camera, POST_NOTIFICATIONS,
notification access, accessibility, overlay, battery optimisation — each with *why it is needed*,
the real granted state (re-read on resume) and a button that opens the correct Android screen.
Nothing is auto-granted (Android does not permit it) and the app runs fine when everything is denied.

## 8. Diagnostics (all checks are real)

Gemini • background service • camera • microphone • accessibility • notification access • contacts •
battery optimisation • **speech-recognition engine** • **TTS engine** • **Room database (real query)** •
**overlay permission** • **previous-run crash report**. Every item is `READY / LIMITED / ERROR` with a
cause and a recommended action.

## 9. Known limitations (deliberately not faked)

* **Floating overlay assistant** (spec §8) is *not* shipped in this branch — the overlay permission is
  surfaced and checked, but no indicator window is drawn, so nothing pretends to exist.
* **Gemini Live full-duplex audio** is not used; voice is real Android `SpeechRecognizer` (STT) →
  Gemini REST → Android TTS. This is genuine voice-to-voice, but it is not a streaming Live session.
* **Wake word ("Hey Arohi")** is not implemented; voice input starts from the mic button/service.
* Automated sending inside third-party chat apps depends on Accessibility and per-app layouts; the
  assistant prepares the message and says so when it cannot send.
* No Android security bypass of any kind (lock screen, banking auth, protected files) is attempted.

## 10. Verification status

* `./gradlew :app:assembleDebug` — **green in CI**, APK published as a workflow artifact/release.
* On-device acceptance testing (Galaxy S8+ / Android 9) still has to be done by the owner: this
  environment has no Android device, emulator, SDK or JDK, so no runtime claim is made here.

# FluidScan Pro

[![Android CI](https://github.com/Mk21875-web/FluidScanPro/actions/workflows/android.yml/badge.svg)](https://github.com/Mk21875-web/FluidScanPro/actions/workflows/android.yml)

An enterprise-grade **Document Scanner & PDF Editor** for Android — combining the core
functionality of CamScanner, Adobe Scan, and iLovePDF. The differentiator is an
**ultra-fluid UI/UX**: every interaction is driven by spring physics, 90fps animations,
and physical motion, built entirely in **Kotlin + Jetpack Compose**.

> Status: feature-complete across all 5 planned phases; builds to a debug APK and is
> verified on CI on every push/PR.

## Features

### 1. Core scanning engine
- Real-time edge detection with **spring-loaded** border snapping + overshoot (damping 0.6).
- Perspective correction via a homography warp (`Matrix.setPolyToPoly`).
- Manual crop with **magnetic snap + haptics**.
- Multi-page capture with **card-stack** drag-and-drop reorder (lift-and-shadow).
- Live filter previews (B&W / Magic Color / Grayscale) with cross-fade, applied as GPU
  `ColorFilter`s (no extra bitmaps).

### 2. PDF construction & full editor
- PDF assembly with **AES-256 password protection** (PDFBox-Android).
- Annotation suite: **Bézier-smoothed** pen, spring-loaded shape previews, IME-inset text boxes.
- Signatures & stamps with **parallax drag** + physical "seal-press" drop animation.
- Page manager with gentle spread/collapse layouts.
- Password dialog with a key-turn Lottie micro-animation.

### 3. Dashboard & file manager
- **Room** persistence; documents saved on export.
- **Grid ↔ List** morphing via `AnimatedContent`.
- Card-to-fullscreen sheet expansion.
- Expanding search bar with **RenderEffect** background blur (debounced query).
- Wave-like cloud-sync progress indicator.

### 4. OCR & AI features
- **ML Kit Text Recognition v2** (offline, multi-script: Latin/Chinese/Devanagari/Japanese/Korean → 100+ languages).
- Line-by-line **highlight animation** during recognition.
- AI Cleanup tool with a **brush-stroke sweep** effect.
- Smart file naming with a **typewriter** text reveal.

### 5. Built-in utilities
- QR/Barcode scanner with a **breathing reticle** that snaps to a checkmark + haptic on detection.
- Batch processing with a visual **assembly-line** UI (QUEUED → CLEANING → RECOGNIZING → PACKAGING → DONE).
- Print/Share with a **paper-plane takeoff** Lottie animation.

## Tech stack

| Area            | Choice |
|-----------------|--------|
| Language / UI   | 100% Kotlin, Jetpack Compose (Material 3, dynamic color) |
| Architecture    | Clean Architecture + MVI, Hilt for DI |
| Async           | Coroutines / Flow — all heavy work on `Dispatchers.IO` |
| Camera          | CameraX (preview + analysis + capture) |
| ML              | ML Kit: Document/Text Recognition, Barcode Scanning |
| PDF             | PDFBox-Android (create / edit / encrypt) + platform `PdfRenderer` |
| Persistence     | Room, DataStore |
| Images          | Coil |
| Animation       | Compose animation APIs + Lottie-Compose |
| Background work | WorkManager |

### Performance principles
- Zero UI-thread blocking — image/PDF/OCR work runs on `Dispatchers.IO`.
- Eager bitmap recycling to avoid leaks.
- Normalized (0..1) annotation geometry so overlays stay aligned across zoom/rotation/DPI.
- Spring physics constants centralized in `ui/theme/Motion.kt` (the single source of the "fluid" feel).

## Project structure

```
com.fluidscan.pro
├── core/      cross-cutting helpers (haptics, motion, blur, bitmap/file utils)
├── data/      Room, remote, repository impls, mappers
├── domain/    pure Kotlin models, repository interfaces, use cases
├── di/        Hilt modules
├── service/   long-running engines (scan, pdf, ocr, sync, utility)
└── ui/        MVI screens + theme + shared components
                (scanner · editor · dashboard · ocr · utilities)
```

Dependency rule: `ui → domain ← data` (domain depends on nothing Android-specific).

## Build

### Requirements
- JDK 17
- Android SDK (compileSdk 35, build-tools 35)
- `minSdk 26`, `targetSdk 35`

### Commands
```bash
# Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Kotlin compile only
./gradlew :app:compileDebugKotlin

# Android lint
./gradlew :app:lintDebug
```

### Firebase (optional, deferred)
Cloud sync via Firebase is wired in the dependency graph but the `com.google.gms.google-services`
plugin is **not applied** until a real `google-services.json` is added. To enable it: drop your
`google-services.json` into `app/`, then re-enable the plugin in `app/build.gradle.kts`.

## CI

GitHub Actions ([`.github/workflows/android.yml`](.github/workflows/android.yml)) runs
`:app:assembleDebug` on every push/PR to `main` and uploads the resulting `app-debug` APK as a
build artifact. `main` is protected by a ruleset that requires this check to pass before merge.

## License

No license has been chosen yet. All rights reserved by the repository owner until a license is added.

# PhotoSwipe

> Tinder-style 4-way swipe photo sorter for Android. Clean up years of camera roll in minutes.

<p align="center">
  <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/license-MIT-22D3EE"></a>
  <img alt="Platform: Android" src="https://img.shields.io/badge/platform-Android%208%2B-3DDC84">
  <img alt="Language: Kotlin" src="https://img.shields.io/badge/language-Kotlin-7F52FF">
  <img alt="UI: Jetpack Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4">
  <img alt="Material 3" src="https://img.shields.io/badge/Material-You-EAB308">
</p>

PhotoSwipe is an open-source Android app that turns gallery cleanup into a satisfying card-stack swipe game. Each photo from your camera roll is one card. Flick it in one of four directions to **delete**, **keep**, **favorite**, or **move to a custom folder**, then move on to the next one. Deletes are batched so Android only asks for confirmation once per batch instead of once per photo.

The project is intentionally small, dependency-light, and 100 % local — no accounts, no analytics, no network calls.

---

## Contents

- [Features](#features)
- [How a swipe works](#how-a-swipe-works)
- [Screenshots](#screenshots)
- [Install](#install)
- [Build from source](#build-from-source)
- [Configuration](#configuration)
- [Permissions](#permissions)
- [Architecture](#architecture)
- [Project layout](#project-layout)
- [FAQ](#faq)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

---

## Features

- **Four-direction swipe gestures** with a color-coded drag overlay that grows with your finger and locks onto the dominant axis.
- **Batched deletes** — left-swipes are collected into a queue and submitted as a single Android delete request per batch (default 10, configurable 1–50).
- **App-managed folders** — create new gallery folders by name, no SAF picker, no permission dance. New folders appear under `Pictures/PhotoSwipe/<name>/` (or `DCIM/PhotoSwipe/<name>/` if you prefer) and are visible in every gallery app.
- **Internal Favorites folder**, auto-seeded on first launch so up-swipe is instantly useful.
- **Single-level Undo** via snackbar after every swipe, including a queue-reinsert when applicable.
- **Date-range and sort-order filters** to focus a session on what matters: newest, oldest, largest, smallest, random, or a fixed time window.
- **Material 3 + dynamic color** (Android 12+) with manual System / Light / Dark override and a toggle for Material You wallpaper colors.
- **Themed monochrome icon** for the Android 13+ themed-icons system setting.
- **Edge-to-edge UI**, haptics, optional direction hints, optional metadata overlay, configurable drag sensitivity and card-stack depth.
- **Privacy-first**: no internet permission, no analytics, no crash reporters, no tracking.
- **Reset everything** to defaults at any time from Settings.

## How a swipe works

| Direction | Action                                                                                  |
| --------- | --------------------------------------------------------------------------------------- |
| ← Left    | Queue the photo for delete. The Android delete prompt appears once your batch fills up. |
| → Right   | Keep the photo and mark it reviewed so it does not reappear next session.               |
| ↑ Up      | Copy the photo into your **Favorites** folder.                                          |
| ↓ Down    | Open a bottom-sheet folder picker; the photo is moved into the chosen folder.           |

The threshold (96 dp by default) and the number of preview cards stacked underneath the active one are both adjustable in Settings.

## Screenshots

Screenshots will be added as part of an upcoming release. In the meantime, the in-app UI consists of three tabs:

- **Swipe** — full-bleed card stack with progress bar, pending-delete banner, and an undo snackbar.
- **Folders** — list of managed folders with create, rename, remove, mark-favorite and mark-default-down actions, plus a FAB to add new folders.
- **Settings** — grouped, icon-tinted rows for filter, behavior, storage, appearance, data and about.

## Install

Pre-built debug APKs are attached to each PR in the [Releases](https://github.com/Leonxlnx/image-sorter-app/releases) section once the corresponding pull request is merged. To sideload:

1. Download the APK to your phone.
2. Tap it, allow "Install unknown apps" for your browser/file manager if prompted.
3. On first launch, grant photo access (`READ_MEDIA_IMAGES`, plus `READ_MEDIA_VIDEO` if you opted into videos).

Release builds for Google Play are not currently distributed; you are encouraged to build from source.

## Build from source

### Prerequisites

- JDK 17 (Temurin, OpenJDK, or Zulu)
- Android SDK with platform 34 and build-tools 34 installed
- ANDROID_HOME / ANDROID_SDK_ROOT pointing at your SDK directory
- An Android device or emulator running Android 8.0 (API 26) or higher

### Build the debug APK

```bash
git clone https://github.com/Leonxlnx/image-sorter-app.git
cd image-sorter-app
./gradlew :app:assembleDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

### Lint and tests

```bash
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest    # if/when unit tests are added
```

### Install onto a connected device

```bash
./gradlew :app:installDebug
```

## Configuration

Every option lives under **Settings**. Each section reflects a separate concern:

| Section        | What you can tune                                                                                  |
| -------------- | -------------------------------------------------------------------------------------------------- |
| **Filter**     | Date range (any, today, 7 days, 30 days, 1 year), sort order, include videos, skip already-reviewed |
| **Behavior**   | Delete batch size, drag sensitivity, card stack depth, haptics, direction hints, metadata overlay  |
| **Storage**    | Folder root (`Pictures/PhotoSwipe` or `DCIM/PhotoSwipe`)                                           |
| **Appearance** | Theme (System / Light / Dark), Material You dynamic color toggle                                   |
| **Data**       | Reset the reviewed list, reset all settings to defaults                                            |
| **About**      | Version, license info, link to the GitHub repository                                               |

Settings are persisted via Jetpack DataStore (`Preferences`).

## Permissions

PhotoSwipe asks only for what it needs.

| Permission                                 | Why                                                                 |
| ------------------------------------------ | ------------------------------------------------------------------- |
| `READ_MEDIA_IMAGES` (Android 13+)          | Read photos from your camera roll                                   |
| `READ_MEDIA_VIDEO` (Android 13+, optional) | Required only when "Include videos" is enabled                      |
| `READ_EXTERNAL_STORAGE` (Android ≤ 12)     | Legacy fallback for reading the media store                         |
| `WRITE_EXTERNAL_STORAGE` (Android ≤ 9)     | Legacy fallback when creating folders on pre-Q devices              |

There is **no `INTERNET` permission**. The app cannot phone home even if it wanted to.

## Architecture

PhotoSwipe is a single-module Compose app built around three repositories and one Application-owned service:

```
MainActivity ──> AppRoot (Compose nav host)
                  ├── SwipeScreen ────── SwipeViewModel ──┐
                  ├── FoldersScreen                       │
                  └── SettingsScreen                      │
                                                          ▼
ImageSorterApp ─┬─ PhotoRepository       (MediaStore queries)
                ├─ FolderRepository      (DataStore-backed list of SortFolder)
                ├─ ReviewedRepository    (DataStore-backed set of IDs)
                ├─ SettingsRepository    (DataStore-backed preferences)
                └─ SortActions           (Keep / EnqueueDelete / CopyTo / MoveTo)
```

Key decisions:

- **No DI framework.** `ImageSorterApp` lazily constructs each repository and exposes them as properties. ViewModels read them via `CreationExtras`.
- **No SAF.** All folder writes go through `MediaStore` with `RELATIVE_PATH` on API 29+ and a `File` + `MediaScannerConnection` fallback on older versions.
- **Batched deletes.** `SortActions` keeps an in-memory pending list. The UI auto-flushes when the batch threshold is reached or when the user taps "Delete now"; flushing calls `MediaStore.createDeleteRequest(...)` and emits a single `IntentSender` for the whole batch.
- **Compose-only navigation.** Three tabs via `NavHost` + a Material 3 `NavigationBar` — no fragments or activity per screen.

## Project layout

```
app/src/main/kotlin/com/leonxlnx/imagesorter/
├── ImageSorterApp.kt        # Application class, owns repositories
├── MainActivity.kt          # Edge-to-edge Compose host, applies the theme
├── data/                    # MediaStore + DataStore-backed repositories
│   ├── DateRange.kt
│   ├── FolderRepository.kt
│   ├── Photo.kt
│   ├── PhotoRepository.kt
│   ├── ReviewedRepository.kt
│   ├── SettingsRepository.kt
│   ├── SortActions.kt
│   └── SortOrder.kt
└── ui/
    ├── AppRoot.kt           # NavHost + bottom navigation
    ├── folders/             # Folder management screen + name dialog
    ├── permission/          # Runtime permission gate
    ├── settings/            # Settings screen with grouped rows
    ├── swipe/               # Card stack, drag detection, view model
    └── theme/               # Material 3 color schemes + ThemeMode
```

## FAQ

**Can PhotoSwipe permanently delete photos without the Android system prompt?**
No. Android 11+ requires the system delete prompt for any media you did not create. PhotoSwipe minimizes the friction by batching deletes — one prompt per N photos — but it cannot suppress the prompt entirely.

**Will it delete my photos by accident?**
A delete is only a delete after you confirm the system prompt. Until the batch flushes, queued photos sit in an in-memory queue you can clear by killing the app, undoing the last swipe, or simply continuing without flushing.

**Where do my folders go?**
By default, into `Pictures/PhotoSwipe/<folder name>/`. You can switch the root to `DCIM/PhotoSwipe/` in Settings → Storage.

**Why does the app need video permission?**
Only if you enable "Include videos" under Settings → Filter. Otherwise, the app never asks for it.

**Does it sync between devices?**
No. PhotoSwipe is single-device, offline-only by design.

## Roadmap

Ideas that may land in future versions:

- Per-folder color tags / icons
- Optional review log (CSV export of what was sorted where)
- Quick-pick chip for a recently-used folder on the swipe screen
- Tablet / foldable two-pane layout
- Localization beyond English
- Automated UI tests with Compose + Maestro

If one of these excites you, see [Contributing](#contributing).

## Contributing

Contributions are welcome. Before you start a non-trivial change, please open an issue describing what you have in mind so we can sanity-check the approach.

A short checklist for pull requests:

1. Fork the repo and create a feature branch off `main`.
2. Run `./gradlew :app:lintDebug :app:assembleDebug` locally and make sure both succeed.
3. Keep changes minimal and aligned with the existing Compose / Material 3 style.
4. Update [CHANGELOG.md](CHANGELOG.md) under an `Unreleased` section.
5. Open a PR following the template — describe **what** changed and **why**.

See [CONTRIBUTING.md](CONTRIBUTING.md) for a longer guide, and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community guidelines.

## Security

Found a security issue? Please **do not** open a public GitHub issue. Instead, follow the disclosure process described in [SECURITY.md](SECURITY.md).

## License

PhotoSwipe is released under the [MIT License](LICENSE). You are free to use, modify, and redistribute it for any purpose, including commercial use. Attribution is appreciated.

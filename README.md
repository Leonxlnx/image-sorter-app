# PhotoSwipe Sorter

A satisfying, Tinder-style Android app for cleaning up your camera roll. Swipe through your
photos and decide what to do with each one in four directions:

| Direction | Action |
|-----------|--------|
| ← Left    | **Delete** the photo. Deletes are queued and confirmed in batches (one Android system dialog per N photos instead of one per photo). |
| → Right   | **Keep** the photo (mark as reviewed, never shown again). |
| ↑ Up      | Copy to your **Favorites** folder (auto-created at `Pictures/PhotoSwipe/Favorites/`). |
| ↓ Down    | **Move** to one of your folders — opens a quick folder picker. |

Built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Targets Android 8.0
(API 26) through Android 14 (API 34).

## Features

- **Swipe-only** card stack — no tap buttons cluttering the UI. Smooth drag, fling, and a
  color-coded direction overlay that grows as you drag.
- **Batched delete confirmation**: the Android system delete dialog appears once per
  configurable batch (default: every 10 swipes), not once per photo. A subtle banner at the
  top of the swipe screen shows how many deletes are queued and lets you confirm immediately.
- **Internal folder management** — create gallery folders directly from the app. Each
  folder lives under `Pictures/PhotoSwipe/<name>/` and is visible in every gallery app.
  No system folder picker required.
- A **Favorites** folder is created automatically on first launch; you can rename it or
  switch the favorite flag to any other folder.
- Single-level **Undo** snackbar after every swipe (also pulls the source out of the
  pending-delete queue).
- **Folders tab** to add, rename, or remove destination folders. Mark one as Favorites
  (used for up-swipe) or as the default down-swipe destination.
- **Settings** for:
  - Date range filter (Any time / Today / Last 7 days / Last 30 days / Last year).
  - Include videos.
  - Skip already-reviewed photos.
  - Delete batch size (1–50).
  - Haptic feedback toggle.
  - On-card direction hints toggle.
  - System / Light / Dark theme.
  - Reset reviewed list.
- Dynamic color theme on Android 12+, hand-tuned dark scheme below that.

## Screens

1. **Swipe** — the main card stack. The top card is draggable in any of four directions
   and flings off-screen on release. The next two cards peek through underneath for depth.
2. **Folders** — list your destination folders. The floating **Add folder** button opens a
   simple name dialog; the folder is created in `Pictures/PhotoSwipe/<name>/` the first
   time you swipe a photo into it.
3. **Settings** — every knob the app exposes.

## How destinations work

Destinations are app-managed. You create them by name in the Folders tab; PhotoSwipe Sorter
writes into `Pictures/PhotoSwipe/<name>/` using **MediaStore inserts** on Android 10+ (no
storage permissions or document tree pickers needed). On Android 9 and below it falls back
to direct `File` writes plus `MediaScannerConnection` so the gallery indexes the new file.

Deletes are deferred: every left-swipe enqueues the source URI. When the queue reaches the
configured batch size — or when you tap **Delete now** on the banner — the app builds a
single `MediaStore.createDeleteRequest` for the whole batch, so Android shows one
confirmation dialog instead of one per photo. The dialog still asks for explicit user
consent (required on Android 11+); only the *frequency* is reduced.

## Permissions requested

| Permission | Why |
|------------|-----|
| `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` | Read your gallery on Android 13+. |
| `READ_MEDIA_VISUAL_USER_SELECTED` | Support partial photo access on Android 14+. |
| `READ_EXTERNAL_STORAGE` | Legacy read on Android 12 and below. |
| `WRITE_EXTERNAL_STORAGE` | Legacy write fallback on Android 9 and below (`maxSdkVersion=28`). |

## Building

### Prerequisites

- JDK 17
- Android SDK Platform 34 + Build-Tools 34 (the Android SDK command-line tools are enough;
  Gradle will install the required components for you).
- Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your SDK location, **or** create a
  `local.properties` file at the repo root with `sdk.dir=/path/to/android-sdk`.

### Build a debug APK

```bash
./gradlew :app:assembleDebug
```

The resulting APK lives at:

```
app/build/outputs/apk/debug/app-debug.apk
```

You can install it directly to a connected device:

```bash
./gradlew :app:installDebug
```

### Lint

```bash
./gradlew :app:lintDebug
```

## CI

A ready-to-use GitHub Actions workflow lives at `docs/android-ci.yml`. To activate it, copy
the file into the standard location (the OAuth app that opened this PR did not have the
`workflow` scope, so the file is parked under `docs/` so the PR could be created):

```bash
mkdir -p .github/workflows
cp docs/android-ci.yml .github/workflows/android.yml
git add .github/workflows/android.yml && git commit -m "Enable Android CI"
```

The workflow runs lint + a debug-APK build on every push and pull request and uploads the
resulting APK as a build artifact called `PhotoSwipeSorter-debug-apk`.

## Project structure

```
app/
└── src/main/
    ├── AndroidManifest.xml
    ├── kotlin/com/leonxlnx/imagesorter/
    │   ├── ImageSorterApp.kt          # Application + simple service locator
    │   ├── MainActivity.kt            # Hosts Compose theme + nav
    │   ├── data/
    │   │   ├── DateRange.kt           # Filter presets and custom range
    │   │   ├── Photo.kt
    │   │   ├── PhotoRepository.kt     # MediaStore queries (images + optional videos)
    │   │   ├── SettingsRepository.kt  # DataStore-backed settings
    │   │   ├── FolderRepository.kt    # User-picked SAF destinations
    │   │   ├── ReviewedRepository.kt  # Tracks already-swiped photo IDs
    │   │   └── SortActions.kt         # Keep / Delete / Copy / Move primitives
    │   └── ui/
    │       ├── AppRoot.kt             # Bottom-nav scaffold
    │       ├── permission/PermissionGate.kt
    │       ├── swipe/                 # Card stack + drag UI
    │       ├── folders/FoldersScreen.kt
    │       ├── settings/SettingsScreen.kt
    │       └── theme/Theme.kt
    └── res/
        ├── drawable/                  # Launcher
        ├── mipmap-anydpi-v26/         # Adaptive icon
        └── values/                    # strings, colors, themes
```

## Roadmap

- Statistics screen (deleted size, time saved).
- Configurable swipe targets per direction.
- Multi-step undo.
- Trash folder mode (instead of system delete).

## License

MIT — see `LICENSE`.

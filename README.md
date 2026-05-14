# PhotoSwipe Sorter

A satisfying, Tinder-style Android app for cleaning up your camera roll. Swipe through your
photos and decide what to do with each one in four directions:

| Direction | Action |
|-----------|--------|
| ← Left    | **Delete** the photo (via the system MediaStore delete dialog on Android 11+) |
| → Right   | **Keep** the photo (mark as reviewed, never shown again) |
| ↑ Up      | Copy to your **Favorites** folder |
| ↓ Down    | **Move** to a folder — opens a folder picker every time so you can decide where it goes |

Built with **Kotlin**, **Jetpack Compose**, and **Material 3**. Targets Android 8.0
(API 26) through Android 14 (API 34).

## Features

- 4-way swipe with smooth drag, color-coded direction overlays, and tactile haptic feedback.
- Action buttons under every card for users who prefer tapping over swiping.
- Single-level **Undo** snackbar after every swipe.
- **Folder picker** for the down-swipe — pick from any folder you've previously registered.
- **Folders tab** to add, rename, or remove destination folders via the Storage Access
  Framework (`OpenDocumentTree`). Designate any folder as the Favorites destination or the
  default move-down destination.
- **Settings** for:
  - Date range filter (Any time / Today / Last 7 days / Last 30 days / Last year).
  - Include videos.
  - Skip already-reviewed photos.
  - Batch size for the delete confirmation flow.
  - Haptic feedback toggle.
  - On-card direction hints toggle.
  - System / Light / Dark theme.
  - Reset reviewed list.
- Dynamic color theme on Android 12+, hand-tuned dark scheme below that.

## Screens

1. **Swipe** — the main card stack. The top card is draggable in any of four directions and
   flings off-screen on release. The next two cards peek through underneath for depth.
2. **Folders** — manage your Storage Access Framework destination folders.
3. **Settings** — every knob the app exposes.

## How destinations work

The app never tries to move files into arbitrary system folders. Instead, you grant it
persistable URI permission on the destinations you care about (typically `DCIM/Favorites`
and `DCIM/ToSort`). PhotoSwipe Sorter copies the source bytes into the destination via the
Storage Access Framework, then asks the system to delete the original (when you swipe down
to move).

## Permissions requested

| Permission | Why |
|------------|-----|
| `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO` | Read your gallery on Android 13+. |
| `READ_MEDIA_VISUAL_USER_SELECTED` | Support partial photo access on Android 14+. |
| `READ_EXTERNAL_STORAGE` | Legacy access on Android 12 and below. |
| `WRITE_EXTERNAL_STORAGE` | Legacy write on Android 10 and below. |
| SAF persistable URIs (granted at runtime, per folder you pick) | Write photos into destination folders. |

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

# Changelog

All notable changes to PhotoSwipe are documented here. This project follows [Semantic Versioning](https://semver.org/) loosely — minor version bumps for new features, patch bumps for fixes, and a major bump only on breaking UX changes.

## [1.3.0] – Launch pass

### Added

- **Full-screen preview**: long-press any card to see the photo full-size before deciding. Tap anywhere or press back to dismiss.
- **Session statistics**: when the queue is empty, the screen shows how many photos you deleted, kept, favorited and sorted in that session.
- **Reduce motion** accessibility toggle under Settings → Behavior. Replaces the spring animations with instant transitions for motion-sensitive users.
- **F-Droid metadata stub** under `fastlane/metadata/android/en-US/` (title, short / full description, changelog) so the app is ready for catalogue submission.
- **PRIVACY.md** documenting the no-data-leaves-the-device policy. Linked from the README with a privacy badge.
- **Data extraction + full backup rules** (`res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml`): cloud backup and device transfer now preserve your settings and folder list and explicitly drop the per-session reviewed photo IDs.
- **Predictive back gesture** support via `android:enableOnBackInvokedCallback="true"` (Android 13+).
- **TalkBack content description** for the photo card that announces the four swipe directions and the long-press preview affordance.

### Changed

- **Tablet / landscape**: the card deck is now capped at 560 dp wide and centered, instead of stretching across the full screen.
- **Edge-to-edge polish**: the activity declares `configChanges` for orientation, screen-size, screen-layout, keyboard-hidden and UI mode so the swipe deck no longer recreates on rotation; insets continue to be handled via Scaffold.
- **Animation specs in `DraggableTopCard`** factored out so reduce-motion users get tween 0 ms / 100 ms instead of the default spring physics.
- Removed the redundant `android:label` on the launcher activity (uses the application label).

### Internal

- Bumped `versionName` to `1.3.0`, `versionCode` to `5`.
- Added `SessionStats` data class and `bumpStats` / `unBumpStats` helpers in `SwipeViewModel`.
- Added `reduceMotion` to `SettingsRepository` (DataStore key `reduce_motion`).
- Added new string resources: `empty_state_summary`, `stats_*`, `settings_reduce_motion*`, `photo_card_a11y`, `preview_close`.

## [1.2.1] – Quiet UX polish

### Changed

- Removed the bottom snackbar that popped up after **every** swipe. The screen stays calm now; only the card animation confirms the action.
- Replaced the per-action snackbar with a transient floating **Undo** pill that slides up briefly after each swipe and auto-fades after 3.5 seconds. Tap it once to restore the last photo.
- Redesigned the batched-delete indicator: instead of a loud full-width banner, the queue now lives as a compact `n / batchSize` pill in the top bar, with a subtle bouncy pulse whenever it increments. Tap the pill to trigger the system delete prompt for the whole batch.
- Switched the card return-to-center and exit animations from linear tweens to spring animations for a more satisfying, physical feel.
- Reordered `PhotoCard` parameters so `modifier` comes before optional parameters, matching the Compose API style.

### Internal

- Bumped `versionName` to `1.2.1`, `versionCode` to `4`.
- Removed unused string resources (`undo_done`, `delete_now`, `pending_delete_status`).

## [1.2.0] – Launch readiness

### Added

- New **Sort order** setting: newest first, oldest first, largest first, smallest first, random.
- New **Drag sensitivity** slider (40–200 dp) to tune how far you have to swipe before a card commits.
- New **Card stack depth** picker (0–3) to control how many preview cards sit behind the active one.
- New **Folder root** setting: keep gallery folders under `Pictures/PhotoSwipe` or move them to `DCIM/PhotoSwipe`.
- New **Photo metadata overlay** toggle to hide the file name / date / size strip on each card.
- New **Material You dynamic color** toggle for Android 12+.
- New **About** section in Settings showing version, build, license blurb, and a link to the GitHub repo.
- New **Reset all settings** action with confirmation dialog.
- New monochrome launcher icon, picked up by Android 13+ themed icons.
- Full open-source documentation: README, CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, CHANGELOG, issue and PR templates.

### Changed

- All UI strings audited for grammar and clarity.
- Settings screen reorganized into Filter, Behavior, Storage, Appearance, Data and About sections, with wrapping chip rows on small screens.
- Launcher icon redesigned around a layered photo-card motif.

### Internal

- Bumped `versionName` to `1.2.0`, `versionCode` to `3`.
- Centralized folder-path construction through `FolderRoot.relativePathPrefix`.

## [1.1.0] – Swipe-only v2 polish

### Added

- Batched deletes: left-swipes queue up and trigger a single Android delete prompt per batch.
- App-managed folders: create gallery folders by name; no SAF picker.
- Internal Favorites folder, auto-created on first launch.
- Pending-delete banner with a "Delete now" action on the swipe screen.

### Changed

- Removed the tap-action buttons under the card; the experience is now swipe-only.
- Removed the corner direction chips that overlapped the photo.
- Settings screen rewritten with icon-tinted rows and per-section grouping.

## [1.0.0] – Initial release

### Added

- Tinder-style 4-way swipe card stack with smooth drag, fling and color-coded overlays.
- MediaStore-backed photo queue with date-range filter, video opt-in, and reviewed-skipping.
- Single-level undo snackbar.
- Three-tab layout: Swipe, Folders, Settings.
- Material 3 theming with System / Light / Dark mode.

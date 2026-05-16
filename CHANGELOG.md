# Changelog

All notable changes to PhotoSwipe are documented here. This project follows [Semantic Versioning](https://semver.org/) loosely — minor version bumps for new features, patch bumps for fixes, and a major bump only on breaking UX changes.

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

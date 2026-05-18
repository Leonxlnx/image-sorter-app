# Privacy Policy

_Last updated: 2024_

PhotoSwipe is an open-source Android app for sorting your local camera roll. The whole point of the app is to stay on your device. This document explains exactly what that means in practice.

## What PhotoSwipe does not do

- It does not send your photos, your file names, your metadata, or any aspect of your library to any server.
- It does not collect analytics, telemetry, crash reports, or usage statistics.
- It does not display ads.
- It does not contain third-party advertising or tracking SDKs.
- It does not require an account, email address, or sign-in of any kind.
- It does not request the `INTERNET` permission.

## What PhotoSwipe stores on your device

- A list of photo IDs that you have already "reviewed" (so you don't see them again next session), stored locally via Android DataStore.
- Your user-tunable settings (batch size, sort order, theme, etc.), stored locally via Android DataStore.
- The list of destination folders you created in-app, stored locally via Android DataStore. The actual folders live in `Pictures/PhotoSwipe/` or `DCIM/PhotoSwipe/` and are visible to every other gallery app on your device.

Nothing in this list is unique to PhotoSwipe in the sense that it could identify you — and none of it leaves your device.

## Permissions

- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED` (Android 13+): required to list and display the photos you swipe.
- `READ_EXTERNAL_STORAGE` (Android 12 and below): same purpose on older devices.
- `WRITE_EXTERNAL_STORAGE` (Android 9 and below): required only on legacy devices to write copies into your gallery folders. On Android 10+ this is replaced by the scoped MediaStore APIs.

Deleting a photo goes through the standard Android system delete dialog (Android 11+), and copies/moves use the standard scoped storage `MediaStore` APIs.

## Backups

When Android backs up the app (cloud backup or device transfer), PhotoSwipe includes your settings and your list of destination folders, but explicitly excludes the per-session "reviewed photo IDs" so a fresh device starts clean. The exact rules are in [`app/src/main/res/xml/backup_rules.xml`](app/src/main/res/xml/backup_rules.xml) and [`app/src/main/res/xml/data_extraction_rules.xml`](app/src/main/res/xml/data_extraction_rules.xml).

## Source

Every line of code that runs on your device is available in this repository under the MIT license. If anything in this policy doesn't match the code, the code is the source of truth — and we'd love a pull request fixing the docs.

## Questions

Open an issue on the [GitHub repository](https://github.com/Leonxlnx/image-sorter-app).

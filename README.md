# DupTrash

Native Android app (Kotlin + Jetpack Compose) for finding and pruning
byte-identical duplicate photos and videos on the device.

## What it does

- **Scan media files** — enumerates the device's photos & videos via
  `MediaStore` and persists `{path, size, mtime, md5}` rows in a local
  SQLite (Room) database. Only files that share an exact byte size with at
  least one other file get hashed (size-grouped MD5 pre-filter). Re-scans
  are incremental — unchanged files keep their cached hash.
- **Scan for duplicates** — groups rows by MD5 and presents the duplicate
  groups in a Compose list with thumbnails, file sizes, full paths, and the
  total reclaimable space.
- **Regex deletion rules** — maintain a list of regex patterns matched
  against the full file path. Any duplicate copy whose path matches an
  enabled pattern becomes a deletion candidate.
- **Safety guarantee** — if every copy in a duplicate group would be
  deleted, the entire group is skipped and surfaced for manual review. At
  least one copy of every file always survives.
- **Move to trash, not permanent delete** — uses
  `MediaStore.createTrashRequest` (API 30+). Trashed items land in
  Android's system media trash (visible in Samsung Gallery's Trash) and
  are restorable for ~30 days.

## Requirements

- Android 8.0 (API 26) or later. Trash deletion requires Android 11
  (API 30) or later.
- `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` (API 33+) or
  `READ_EXTERNAL_STORAGE` (API ≤ 32).

## Build

```bash
./gradlew assembleDebug
# APK lands at app/build/outputs/apk/debug/duptrash-<version>-debug.apk
```

## CI / distribution

`.github/workflows/build.yml` builds a debug APK on every push and PR.
Pushes to `main` additionally upload the APK to OneDrive under
`/DupTrash/<filename>` via the Microsoft Graph API. Required secrets:

- `ONEDRIVE_CLIENT_ID`
- `ONEDRIVE_CLIENT_SECRET`
- `ONEDRIVE_REFRESH_TOKEN`

A manual workflow dispatch with `release=true` additionally builds a
signed release APK and creates a GitHub Release tagged `v<version>`.

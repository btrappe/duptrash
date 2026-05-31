# DupTrash

Native Android app (Kotlin + Jetpack Compose) for finding and pruning
byte-identical duplicate photos and videos on the device, with a split
keeper/victim view that makes triage fast.

## What it does

- **Scan media files** — enumerates the device's photos & videos via
  `MediaStore` and persists `{path, size, mtime, md5}` rows in a local
  SQLite (Room) database. Only files that share an exact byte size with at
  least one other file get hashed (size-grouped MD5 pre-filter). Re-scans
  are incremental — unchanged files keep their cached hash.
- **Extra folders (SAF)** — add any folder Android's gallery doesn't show
  (backup dirs with `.nomedia`, SD-card backups, USB-OTG drives) via the
  system folder picker. Picks are stored in SharedPreferences and the
  per-folder grant is taken via `takePersistableUriPermission`, so the
  selection survives app restarts, device reboots, and Google-Drive
  auto-backup restores. The scanner walks these folders with
  `DocumentsContract` and dedupes against MediaStore by absolute path.
- **Scan for duplicates** — groups rows by MD5 and presents a split-view
  screen: the top panel shows the victims (everything-but-one per group),
  the bottom panel shows the chosen keeper, both scrolling in lockstep so
  each group's keeper and victims stay row-aligned. A drag handle between
  the panels lets you resize the split.
- **Keeper picker** — for every duplicate group the app picks a single
  keeper automatically using a five-tier hierarchy. Each keeper is tagged
  with a colored badge so you see *why* it was chosen:
  1. **USER_OVERRIDE** (teal) — you manually picked this one.
  2. **REGEX** (green) — your regex deletion patterns matched all the
     other copies, leaving exactly one survivor.
  3. **NAME** (purple) — copies in the same folder share a name modulo
     Android's `(N)` rename pattern (e.g. `IMG_001.jpg`,
     `IMG_001(1).jpg`, `IMG_001(2).jpg`). The lowest-numbered or
     unnumbered original wins.
  4. **SIMILARITY** (blue) — picks the candidate whose path shares the
     longest common prefix with keepers already chosen in earlier groups,
     so the overall keeper set clusters in coherent folders.
  5. **RANDOM** (amber) — none of the above applied; the canonical
     shortest path is taken and the badge is tappable, opening a picker
     so you can override. Overrides cascade — every SIMILARITY pick
     re-evaluates against the updated keeper pool.
- **Random-picks triage** — a dedicated screen lists only RANDOM-tagged
  groups so you can quickly resolve the ambiguous cases.
- **Regex deletion rules** — maintain an ordered priority list of regex
  patterns matched against the full file path. Any copy whose path
  matches an enabled pattern is a deletion candidate for the planner.
- **Move to trash, not permanent delete** — for MediaStore-backed files
  the app uses `MediaStore.createTrashRequest` (API 30+). Trashed items
  land in Android's system media trash (visible in Samsung Gallery's
  Trash) and are restorable for ~30 days. SAF-backed files (from Extra
  folders) have no system trash and are deleted permanently — the
  bottom-bar warns you when any SAF files are in the deletion plan.

## Requirements

- Android 8.0 (API 26) or later. Trash deletion requires Android 11
  (API 30) or later.
- `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` (API 33+) or
  `READ_EXTERNAL_STORAGE` (API ≤ 32). Extra folders need no manifest
  permission — the per-folder grant is captured at runtime via SAF.

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

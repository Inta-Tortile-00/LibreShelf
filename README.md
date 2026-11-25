# LibreShelf
An android compatible widgetry that looks friendly, and compatible with a wide array of e-book viewers.

# LibreShelf 0.2.0 — development preview

A local-first floating Android bookshelf for EPUB, PDF and CBZ. GPL-3.0-only. No network permission, advertising, analytics, root or broad storage permission.

## Build on Windows
1. Open this directory in Android Studio with JDK 17 and Android SDK Platform 36 installed.
2. Use Gradle 8.11.1 (AGP 8.10.1). This source distribution does not include the Gradle wrapper binary. If needed, install Gradle 8.11.1 and run `gradle wrapper --gradle-version 8.11.1` from the project root, then `gradlew.bat assembleDebug`.
3. Sync and Run on a device, or use Build > Build APK(s). Debug APK: app/build/outputs/apk/debug/app-debug.apk.
4. Add a folder or individually pin books, then add the LibreShelf widget using the launcher widget picker. Tap a book to open it; tap the shelf plank to open settings. Resize the widget to fit more rows.

## Scope
- Global shelf configuration shared by widget instances; manual refresh; alphabetical ordering with pins first.
- EPUB OPF title/creator and EPUB 2/3 embedded covers; PDF first page with filename title; CBZ first natural-sorted image and optional ComicInfo title/writer.
- SAF persistent read grants; external content URI intents; reader preference per book, format or global.
- Missing or unreadable covers use generated artwork. Some readers do not accept document URIs or CBZ MIME types. DRM is not bypassed.
- Index is a local JSON preference store, not Room. Very large libraries, URI alias deduplication, folder removal, custom ordering, pagination, per-widget configuration, periodic scans, robust scan cancellation and PDF metadata are future work. The app list displays up to 500 matches; the widget displays only books fitting its rows. Removing a folder book from the index does not exclude it from subsequent scans.

## Release gate
Not compiled or device-tested in the authoring environment (no Android SDK/Gradle). Test API 28 and API 36, launcher sizing and bitmap memory, rotation, process death, storage revocation, large/malformed archives, reader compatibility, accessibility and Play pre-launch checks. Review temporary archive disk use and scan lifecycle. Choose a permanent application ID before publishing; org.libreshelf is provisional. Generate signed AAB in Android Studio, keep signing secrets outside the repository. Complete Play Data safety, privacy policy, content rating, screenshots and any required testing. This is not a production release.

## 0.2.0 upgrade

Install over 0.1.0 with the same signing key. Package and library preference keys are unchanged. Open Shelf appearance & mode to try the live preview, wood grain, shadow softness, seven materials and rotation settings. Apply commits changes; Cancel leaves the shelf unchanged. Previous/next arrows appear when additional pages exist. Rotation is off by default; 15/30/60/120-minute inexact alarms run when the device is awake and can be delayed by Android. Keep pins visible reserves up to capacity minus one slots; overflow pinned books rotate with other books. Page position is stored separately per widget, but visual settings are shared. No continuous animation or exact alarm permission is used. Covers remain uniformly scaled. This source update has not been built against an Android SDK in the generation environment.

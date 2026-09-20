# ToolNexa

ToolNexa is a native Android utility app focused on practical tools with a clean, light and mobile-first experience.

## v1.3.0

- Image Compressor
- Image Resizer
- Searchable 2-column tool grid
- Firebase Authentication with Google, e-mail/password and international phone SMS verification
- Password recovery by e-mail
- Phone login with country selector, 9-digit local number field, 6-digit SMS code, resend cooldown and number change flow
- Three-stage tool workflow: choose file, personalize, result/preview/save/share
- User-selected Nexauren X folder outside Android app storage, organized by category/tool
- Firebase Cloud Messaging subscription for app update notifications
- Firebase Analytics for screens and app actions
- Account page with Firebase profile metadata
- In-tool processing animation and result previews
- Save to Pictures/ToolNexa and share generated files
- GitHub Release update checker with visible download progress

## Firebase

- Firebase project is configured for package `com.toolnexa.app`.
- Phone sign-in must be enabled in Firebase Authentication and the SMS region policy must allow the target countries.
- Google Sign-In uses the Firebase web OAuth client generated in `google-services.json`.
- Analytics events are normalized for Firebase event-name and parameter limits.

## Android stack

- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- Java 17
- compileSdk 37
- targetSdk 36

## CI and releases

Every push to `main` runs the Android build and updates the matching GitHub Release tag from `versionName`.

When the four `TOOLNEXA_*` signing secrets are present, CI produces a signed release APK using the permanent ToolNexa signing key. Without them, CI falls back to a debug APK for development validation.

Signing material must never be committed to the repository.

## Automatic updates

The app checks the latest GitHub Release. When a newer version is available, it shows an update dialog, downloads the APK with progress feedback and hands it to Android's package installer.

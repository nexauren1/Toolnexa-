# ToolNexa

ToolNexa is a native Android utility app.

## v1.0.0

The first version starts with one real tool:

- Image Compressor
- Image picker
- Quality control from 20% to 100%
- Size comparison
- Save to Pictures/ToolNexa
- Share the compressed image
- Professional light UI
- Navigation menu
- Account placeholder
- Settings
- About
- GitHub Release update checker
- Blocking update dialog when a newer Release is detected
- Download progress from 0% to 100%
- Android package installer handoff

## Roadmap

The first build intentionally has no authentication or payments.

Future layers:

1. Firebase Authentication
2. User profiles
3. Cloudflare Worker + D1
4. PayPal Sandbox
5. Free / Pro / Premium plans
6. Credits and subscription status

## Android updates

The app checks:

https://api.github.com/repos/nexauren1/Toolnexa-/releases/latest

When a newer release tag is detected, the app blocks normal navigation with an update dialog, downloads the APK with visible progress, and hands the file to Android's package installer.

## Signing

The workflow can build a proper signed release when these GitHub Actions secrets are added:

- TOOLNEXA_KEYSTORE_BASE64
- TOOLNEXA_STORE_PASSWORD
- TOOLNEXA_KEY_ALIAS
- TOOLNEXA_KEY_PASSWORD

Without those secrets, the workflow creates a debug APK so the project can be tested immediately. Before production distribution, configure a private signing key and never publish it in the repository.

## Build

The project uses Android Gradle Plugin 9.4.0, Gradle 9.6.0 and Java 17.

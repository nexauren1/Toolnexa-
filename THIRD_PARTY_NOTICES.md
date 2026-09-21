# Third-party notices

## Spotify Basic Pitch

ToolNexa Audio → MIDI neural transcription uses Spotify Basic Pitch
(@spotify/basic-pitch) version 1.0.1. The bundled TFLite model is from Spotify Basic Pitch's
ICASSP 2022 model serialization.

Copyright Spotify AB.

Licensed under the Apache License, Version 2.0.

Source:
https://github.com/spotify/basic-pitch

Package:
https://www.npmjs.com/package/@spotify/basic-pitch

The Basic Pitch TFLite model is fetched from the official Spotify Basic Pitch
repository at build time and bundled into the Android application assets. At
runtime the model executes locally on the device; the Audio → MIDI feature does
not require a WebView, JavaScript runtime, or model CDN access.

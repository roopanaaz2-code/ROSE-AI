# ROSE AI — Android assistant

ROSE AI is a standalone Android project, separate from NightLink and Salman AI Commander.

## Current build scope
- Native Android app shell with a dark rose/violet Compose interface.
- Push-to-talk speech input through the speech recognition service installed on the device.
- Android TextToSpeech output using the device's installed voice.
- Four selectable conversation styles: Cheerful, Caring, Playful, and Calm.
- Basic local replies that work without an internet connection.
- Optional accessibility-service declaration; the user must enable it manually in Android Settings.
- GitHub Actions builds and verifies a debug APK.

## Honest feature status
This is the first installable foundation, not yet a complete autonomous AI assistant.

| Capability | Current status |
|---|---|
| App UI and typed local chat | Implemented |
| Local canned replies | Implemented |
| Device TextToSpeech | Implemented; voice depends on installed Android voices |
| Push-to-talk speech recognition | Uses the device recognition service; offline availability depends on the installed service and language pack |
| Online AI conversations | Not yet connected |
| Downloadable offline AI model | Not yet integrated |
| Wake word / always-on listening | Not implemented |
| Foreground microphone service | Not implemented |
| Alarm / app-launch command routing | Not implemented |
| Screen-reading or gesture-based accessibility actions | Not implemented |
| Camera hand/eye tracking | Not implemented |

The app does not secretly listen in the background. Microphone use begins only after the user taps the microphone. Accessibility is optional and does not retrieve screen contents in this starter.

## Android compatibility
- Minimum supported API: Android 8.0 (API 26).
- Build target: Android 15 (API 35).
- Actual speech recognition, voice availability, and OEM background restrictions vary by device. No claim is made that every feature works identically on every Android phone.

## Build and release
GitHub Actions runs on pushes to main and manual dispatch. It builds app-debug.apk, verifies its APK signature, calculates a SHA-256 checksum, and uploads the APK as a workflow artifact. A tag such as v0.1.0 also creates a GitHub prerelease containing the APK.

## Next milestones
1. Persist chat history and settings locally.
2. Add a pluggable online AI provider with secure key handling and clear network/offline status.
3. Integrate an on-device model only after checking model size, device RAM, CPU/NPU support, and license.
4. Add safe Android intents for supported tasks, with confirmation before consequential actions.
5. Add an opt-in wake-word foreground service with visible notification and explicit permissions.
6. Add foreground-only CameraX / MediaPipe gesture features with camera indicators and calibration.
7. Test on physical devices across API levels before declaring broad compatibility.

## Security principles
- No API secrets are embedded in the APK.
- No background microphone capture in this milestone.
- Sensitive actions require explicit user confirmation.
- Accessibility access must be enabled by the user in Android Settings.

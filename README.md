# ROSE AI — Personal Android Voice Assistant

ROSE AI is a standalone Android app. It does not modify NightLink or Salman AI Commander.

## Included in this build
- Native Compose interface with rose/violet styling.
- Push-to-talk speech input using Android SpeechRecognizer.
- Spoken replies using Android TextToSpeech.
- Four selectable voice styles (Cheerful, Caring, Playful, Calm), with adjusted speech pitch/rate.
- On-device chat-history and personality persistence.
- Offline command engine: current time/date, basic arithmetic, open Android settings/Wi-Fi settings, launch installed apps, and prepare an alarm in the system clock app.
- Dialler action opens the phone dialler for user review; ROSE never places a call itself.
- Optional OpenAI-compatible online chat using the user's own API key.
- API key encrypted with Android Keystore AES-GCM; key is not hardcoded in the source or APK.
- Microphone is activated only by tapping the microphone button.

## Online AI setup
1. Install ROSE and open Settings.
2. Choose **Configure online AI**.
3. Enter your own OpenAI API key and save it.
4. Online requests use the gpt-4o-mini model. Provider usage may incur charges. The app does not supply an API key or pay for API usage.

## Offline behavior and limitations
Offline mode provides the built-in command engine and device TTS. Speech-to-text offline availability depends on the Android speech recognition provider and downloaded language pack. This version does **not** include a downloadable on-device generative language model; open-ended AI chat requires the online provider.

## Not included yet
- Wake word / always-on background listening and its foreground-service notification.
- Camera-based hand or eye tracking.
- General screen reading or autonomous accessibility gestures.
- Full multilingual offline speech/model packages.
- Cloud account sync.

These features require additional model assets, device-specific validation, and explicit permissions. ROSE does not silently listen or perform sensitive actions without user review.

## Android compatibility
- Minimum SDK: Android 8.0 (API 26).
- Compile/target SDK: Android 15 (API 35).
- Speech services, installed voices, app-launch visibility and OEM battery policies vary. Test on the target phone before relying on critical actions.

## Build
GitHub Actions builds the debug APK on pushes to `main` and on manual workflow dispatch. The workflow checks required files, builds the APK, verifies the APK signature, and uploads it as an artifact. Push a version tag such as `v0.2.0` to publish a GitHub prerelease with the APK.

## Privacy and safety
- Chat history and settings are stored locally on the phone.
- The optional online API key is encrypted using Android Keystore.
- Online chat content is sent to OpenAI only when online mode is configured and a message is submitted.
- Calls open the dialler; alarms open the clock UI. ROSE does not silently call, send SMS, make purchases, or change sensitive settings.
- Accessibility service remains optional and does not read screen content in this version.

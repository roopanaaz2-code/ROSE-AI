# ROSE AI — Personal Android Voice Assistant

ROSE AI is a standalone Android app. It does not modify NightLink or Salman AI Commander.

## Included in this build
- Native Compose interface with rose/violet styling.
- Push-to-talk speech input using Android SpeechRecognizer.
- Spoken replies using Android TextToSpeech; ROSE prefers an installed voice marked female when the device TTS engine exposes one. Pitch and rate adapt to the selected style.
- Four selectable voice styles: Cheerful, Caring, Playful, and Calm.
- On-device chat history and personality persistence.
- Offline command engine: current time/date, basic arithmetic, open Android settings/Wi-Fi settings, launch installed apps, and prepare an alarm in the system clock app.
- Optional accessibility navigation commands: back, home, recent apps, vertical scrolling, horizontal swipes, notifications, quick settings, and screenshots on supported Android versions. The user must explicitly enable ROSE in Android Accessibility settings first.
- Optional front-camera Gesture Mode. The camera starts only after the user opens Gesture Mode, taps Start camera, and grants camera permission. On-device wrist movement can trigger scrolling and horizontal swipes; eye-open classification can report an experimental blink signal. Camera frames are not uploaded.
- Dialler action opens the phone dialler for user review; ROSE never places a call itself.
- Optional OpenRouter online chat using the user's own API key and the free-model router; recent conversation turns are included for continuity. Free model availability and quotas are controlled by OpenRouter and may change.
- API key encrypted with Android Keystore AES-GCM; key is not hardcoded in the source or APK.
- Microphone is activated only by tapping the microphone button.

## Online AI setup
1. Install ROSE and open Settings.
2. Choose **Configure online AI**.
3. Enter your own OpenRouter API key (starts with `sk-or-`) and save it.
4. Online requests use OpenRouter's `openrouter/free` model router. Free models can have daily or per-minute limits, may be unavailable, and may change. ROSE does not include a shared API key.

## Accessibility navigation setup
1. Say or type **open accessibility settings**.
2. Find ROSE AI in the installed accessibility services.
3. Enable it only if you want the navigation commands listed above.
4. You can disable the service again from the same Android settings screen.

The accessibility service is intentionally limited to explicit navigation and gesture actions. It does not read, store, or transmit screen contents. Some actions require the optional service to be enabled; unsupported actions depend on Android version and device policy. Android may show a system warning when enabling any accessibility service.

## Offline behavior and limitations
Offline mode provides the built-in command engine and device TTS. Speech-to-text offline availability depends on the Android speech recognition provider and downloaded language pack. This version does **not** include a downloadable on-device generative language model; open-ended AI chat requires the online provider. Female voice selection depends on voices installed by Android's TTS engine.

## Not included yet
- Wake word / always-on background listening and its foreground-service notification.
- Precise directional eye-gaze tracking; current camera support reports experimental blink/eye-open signals only, not gaze direction.
- Fine-grained finger gestures such as pinching or tapping; current gesture controls use wrist movement rather than a dedicated hand-landmark model.
- General screen reading, arbitrary tap automation, or unattended phone actions.
- An integrated offline generative language model.

# ROSE AI — Personal Android Voice Assistant

ROSE AI is a standalone Android app. It does not modify NightLink or Salman AI Commander.

## Included in this build
- Native Compose interface with rose/violet styling.
- Push-to-talk speech input using Android SpeechRecognizer.
- Spoken replies using Android TextToSpeech; ROSE prefers an installed voice marked female when the device TTS engine exposes one. Pitch and rate adapt to the selected style.
- Four selectable voice styles: Cheerful, Caring, Playful, and Calm.
- On-device chat history and personality persistence.
- Offline command engine: current time/date, basic arithmetic, open Android settings/Wi-Fi settings, launch installed apps, and prepare an alarm in the system clock app.
- Optional accessibility navigation commands: go back, go home, open recent apps, scroll up, and scroll down. The user must explicitly enable ROSE in Android Accessibility settings first.
- Dialler action opens the phone dialler for user review; ROSE never places a call itself.
- Optional OpenAI-compatible online chat using the user's own API key; recent conversation turns are included for continuity.
- API key encrypted with Android Keystore AES-GCM; key is not hardcoded in the source or APK.
- Microphone is activated only by tapping the microphone button.

## Online AI setup
1. Install ROSE and open Settings.
2. Choose **Configure online AI**.
3. Enter your own OpenAI API key and save it.
4. Online requests use the gpt-4o-mini model. Provider usage may incur charges. The app does not supply an API key or pay for API usage.

## Accessibility navigation setup
1. Say or type **open accessibility settings**.
2. Find ROSE AI in the installed accessibility services.
3. Enable it only if you want the navigation commands listed above.
4. You can disable the service again from the same Android settings screen.

The accessibility service is intentionally limited to explicit navigation commands. It does not read, store, or transmit screen contents. Android may show a system warning when enabling any accessibility service.

## Offline behavior and limitations
Offline mode provides the built-in command engine and device TTS. Speech-to-text offline availability depends on the Android speech recognition provider and downloaded language pack. This version does **not** include a downloadable on-device generative language model; open-ended AI chat requires the online provider. Female voice selection depends on voices installed by Android's TTS engine.

## Not included yet
- Wake word / always-on background listening and its foreground-service notification.
- Camera-based hand or eye tracking.
- General screen reading, arbitrary tap automation, or unattended phone actions.
- An integrated offline generative language model.

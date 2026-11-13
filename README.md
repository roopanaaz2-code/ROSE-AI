# ROSE AI — Personal Android Voice Assistant

ROSE AI is a standalone Android app. It does not modify NightLink or Salman AI Commander.

## Included in this build
- Native Compose interface with rose/violet styling.
- Push-to-talk speech input using Android SpeechRecognizer, with selectable English (en-US), Urdu (ur-PK), and Pashto (ps-AF) recognition locales.
- Optional user-started wake phrase mode (Hey ROSE, Hello ROSE, and supported Urdu/Pashto wake phrases) using a foreground microphone service and persistent Stop notification.
- Spoken replies using Android TextToSpeech; ROSE prefers an installed voice marked female when the device TTS engine exposes one. Pitch and rate adapt to the selected style.
- Four selectable voice styles: Cheerful, Caring, Playful, and Calm.
- On-device chat history and personality persistence.
- Offline command engine: current time/date, basic arithmetic, open Android settings/Wi-Fi settings, launch installed apps, and prepare an alarm in the system clock app.
- Optional accessibility navigation commands: go back, go home, open recent apps, scroll up, and scroll down. The user must explicitly enable ROSE in Android Accessibility settings first.
- Dialler action opens the phone dialler for user review; ROSE never places a call itself.
- Optional OpenAI-compatible online chat using the user's own API key; recent conversation turns are included for continuity.
- API key encrypted with Android Keystore AES-GCM; key is not hardcoded in the source or APK.
- Microphone is activated by push-to-talk or, when explicitly enabled, by the visible foreground wake-word service.

## Online AI setup
1. Install ROSE and open Settings.
2. Choose **Configure online AI**.
3. Enter your own OpenAI API key and save it.
4. Online requests use the configured OpenRouter free-model route. Free model availability and rate limits can change. The app does not supply an API key or pay for API usage.

## Accessibility navigation setup
1. Say or type **open accessibility settings**.
2. Find ROSE AI in the installed accessibility services.
3. Enable it only if you want the navigation commands listed above.
4. You can disable the service again from the same Android settings screen.

The accessibility service is intentionally limited to explicit navigation commands. It does not read, store, or transmit screen contents. Android may show a system warning when enabling any accessibility service.

## Offline behavior and limitations
Offline mode provides the built-in command engine and device TTS. Speech-to-text offline availability depends on the Android speech recognition provider and downloaded language pack. This version does **not** include a downloadable on-device generative language model; open-ended AI chat requires the online provider. Female voice selection depends on voices installed by Android's TTS engine.

## Wake-word mode and language setup
1. Open ROSE Assistant and choose English, Urdu, or Pashto under **Speech language**.
2. Turn on **Wake word: Hey ROSE** while the app is open and grant microphone permission.
3. Keep the ongoing notification visible; say **Hey ROSE** (or a supported localized wake phrase), then speak a built-in phone command. The notification's **Stop listening** action turns the microphone service off.
4. Turn off the switch in ROSE to stop wake-word mode.

Wake mode uses Android SpeechRecognizer in a foreground microphone service. It is not a dedicated low-power hardware hotword detector; recognition availability, language support, network use, and battery consumption depend on the Android speech provider and device. Android may prevent microphone foreground-service startup when the app is already backgrounded, so enable it from the open app.

## Not included yet
- Camera-based hand or eye tracking.
- General screen reading, arbitrary tap automation, or unattended phone actions.
- An integrated offline generative language model.

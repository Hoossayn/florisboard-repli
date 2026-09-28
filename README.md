# Repli Keyboard

Repli Keyboard is an Android keyboard built from [FlorisBoard](https://github.com/florisboard/florisboard). This [fork](https://github.com/Hoossayn/florisboard-repli) brings Repli's offline English word suggestions, conservative autocorrect, and on-device adaptive learning to FlorisBoard's active keyboard. The current keyboard layout is English QWERTY. The UI uses Repli's theme (warm ivory paper `#FAF8F5`, ink `#27243A`, indigo accent `#6654D1`, lilac highlights) for the app, day keyboard stylesheet, and launcher branding.

## What works

- Word completions and next-word suggestions in eligible English text fields.
- Candidate-tap replacement and autocorrect when a space is pressed.
- A bounded adaptive model for committed words and short phrases. It is encrypted with Android Keystore and stored in the app's no-backup directory.
- A **Typing → Adaptive learning** setting to stop learning or clear saved words. Password, email address, URL, no-suggestions, and incognito fields are excluded.
- A **Repli account** setting backed by Firebase email/password sign-in. Cloud replies stay off unless the build is configured and you sign in; network is used only to exchange the sign-in for a short-lived session and to send captured text you explicitly approve to your first-party backend.
- **Repli chats**: save name-and-tone profiles (Casual/Warm/Direct), cycle tones, remove chats. Notification senders are suggested, never trusted until confirmed; groups and stale/ambiguous chats are excluded.
- **Repli replies**: cloud toggle with per-request approval, opt-in notification access (one-to-one WhatsApp/Telegram only), and guided capture via an accessibility overlay that never reads content, gestures, types, or sends. Screen frames stay in memory; captures finish on Done, timeout, or frame limit.
- On-device ML Kit Smart Reply with learned-style post-processing and automatic fallback; server-mediated generation only after approval. Voice guidance transcription is on-device only; audio is never saved or uploaded.
- Keyboard integration: a **Suggest replies** smartbar action opens the Repli replies panel (sender confirm chips, tap-to-insert suggestions, context review with speaker correction, reply-direction guidance, and per-request cloud approval). Captures reuse the same consent → MediaProjection → OCR pipeline; replies insert as editable text and are never sent automatically.

This is an early keyboard base. It is a separate Android app (`com.replyai.repli.keyboard`) and does not migrate learned data from the existing Repli app. The merged FlorisBoard alpha branch's older suggestion path was incompatible with its active keyboard, so this fork connects the Repli engine to the current keyboard controller.

## Build and test

Use Android SDK 37 and JDK 17, then run:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --tests "dev.patrickgold.florisboard.ime.nlp.latin.repli.WordPredictionEngineTest" --tests "dev.patrickgold.florisboard.repli.account.RepliAccountSessionRepositoryTest"
```

Optional cloud replies need a backend plus Firebase public mobile config. Copy `firebase.local.properties.example` to the ignored `firebase.local.properties` (or pass `-PASSISTED_*` Gradle properties / env vars) and fill `ASSISTED_REPLY_BACKEND_URL` plus the three `ASSISTED_FIREBASE_*` values. Without them the account screen reports unavailable and everything stays on-device. Never commit OpenAI keys, backend signing secrets, or Firebase Admin credentials.

The debug build uses the application ID `com.replyai.repli.keyboard.debug`. On an emulator, the candidate row has been checked with `teh` → `the`, including candidate selection and correction on space. Adaptive storage, its off setting, and deletion were also checked on an emulator. Physical-device testing is still needed.

## Privacy and attribution

Read the [Repli Keyboard privacy note](docs/repli-privacy.md). The original FlorisBoard README is kept as [upstream documentation](UPSTREAM_README.md); its store and download links refer to FlorisBoard, not Repli Keyboard.

FlorisBoard source is Apache-2.0 licensed. The bundled English dictionary is GPL-3.0 licensed; its source revision and hashes are in [dictionary provenance](third_party/provenance/repli-dictionary.json). The dictionary's license text is included in the app assets.

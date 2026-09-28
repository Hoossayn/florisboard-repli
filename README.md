# Repli Keyboard

Repli Keyboard is an Android keyboard built from [FlorisBoard](https://github.com/florisboard/florisboard). This [fork](https://github.com/Hoossayn/florisboard-repli) brings Repli's offline English word suggestions, conservative autocorrect, and on-device adaptive learning to FlorisBoard's active keyboard. The current keyboard layout is English QWERTY.

## What works

- Word completions and next-word suggestions in eligible English text fields.
- Candidate-tap replacement and autocorrect when a space is pressed.
- A bounded adaptive model for committed words and short phrases. It is encrypted with Android Keystore and stored in the app's no-backup directory.
- A **Typing → Adaptive learning** setting to stop learning or clear saved words. Password, email address, URL, no-suggestions, and incognito fields are excluded.

This is an early keyboard base. It is a separate Android app (`com.replyai.repli.keyboard`) and does not migrate learned data from the existing Repli app. The merged FlorisBoard alpha branch's older suggestion path was incompatible with its active keyboard, so this fork connects the Repli engine to the current keyboard controller.

## Build and test

Use Android SDK 37 and JDK 17, then run:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --tests dev.patrickgold.florisboard.ime.nlp.latin.repli.WordPredictionEngineTest
```

The debug build uses the application ID `com.replyai.repli.keyboard.debug`. On an emulator, the candidate row has been checked with `teh` → `the`, including candidate selection and correction on space. Adaptive storage, its off setting, and deletion were also checked on an emulator. Physical-device testing is still needed.

## Privacy and attribution

Read the [Repli Keyboard privacy note](docs/repli-privacy.md). The original FlorisBoard README is kept as [upstream documentation](UPSTREAM_README.md); its store and download links refer to FlorisBoard, not Repli Keyboard.

FlorisBoard source is Apache-2.0 licensed. The bundled English dictionary is GPL-3.0 licensed; its source revision and hashes are in [dictionary provenance](third_party/provenance/repli-dictionary.json). The dictionary's license text is included in the app assets.

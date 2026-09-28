# Repli Keyboard privacy

Repli Keyboard's English suggestion and autocorrect engine runs on the device. Its bundled dictionary is part of the app. The adaptive model learns words and short word sequences only when you commit text in eligible English text fields. It does not learn from password, email address, URL, no-suggestions, or incognito fields.

The learned model is bounded and saved in encrypted storage using Android Keystore. Its file is placed in Android's no-backup directory. You can turn learning off or delete learned words in **Settings → Typing → Adaptive learning**. Turning learning off stops using and updating that model; deleting clears the saved model.

The keyboard inherits other FlorisBoard features, such as clipboard history and extension management. Those features have their own settings and storage behavior. The Repli suggestion and adaptive learning code does not send typed text to a server.

Source code and issue reports: [Hoossayn/florisboard-repli](https://github.com/Hoossayn/florisboard-repli). Repli Keyboard is based on FlorisBoard; upstream information is at [florisboard.org](https://florisboard.org/).

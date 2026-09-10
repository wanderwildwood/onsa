# Privacy

Tuning Fork listens to your instrument and has no way to tell anyone what it heard.

## One permission

`app/src/main/AndroidManifest.xml` declares exactly one:

```
android.permission.RECORD_AUDIO
```

A tuner has to hear the note. The audio is analysed for pitch as it arrives and is not
written anywhere — there is no recording, no buffer kept, no file.

There is **no `INTERNET` permission**. Without it Android will not let the app open a
network connection, so nothing it hears could leave the phone even by accident.

The microphone is open only while the tuner screen is in front.

## What is stored

Your settings, your instruments and your temperaments — reference frequency, temperament,
tolerance, notation, and any custom scales you enter. All local, all yours, and all
exportable and deletable from within the app.

## No analytics

No crash reporting, no telemetry, no advertising identifier, no third-party analytics SDK.

## Checking for yourself

```
aapt2 dump badging app-release.apk | grep uses-permission
```

That prints every permission the built app actually carries. `RECORD_AUDIO` should be the
only one that means anything, and there should be no `INTERNET` in the list.

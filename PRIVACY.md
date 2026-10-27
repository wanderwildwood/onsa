# Privacy

Tuner listens to your instrument and has no way to tell anyone what it heard.

## One permission

`app/src/main/AndroidManifest.xml` declares exactly one:

```
android.permission.RECORD_AUDIO
```

A tuner has to hear the note. The audio is analysed for pitch as it arrives and is not
written anywhere by default. The one exception is the optional recording in Settings: when
you switch it on, the last few seconds are held in memory while the tuner is open, and a file
is written only when you press the record button and choose where to save it.

There is **no `INTERNET` permission**. Without it Android will not let the app open a
network connection, so nothing it hears could leave the phone even by accident.

The microphone is open only while the tuner, the instrument editor or the reference-pitch
"Detect" button is in front, and stops when you leave.

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

## Terms

This app is free software under the GNU General Public License v3 or later. It comes with no
warranty: use it at your own risk. The same text is under About in the app, with a list of
the libraries it uses and their licences.

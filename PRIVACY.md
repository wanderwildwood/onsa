# Privacy

Tuner listens to your instrument and has no way to tell anyone what it heard.

## One permission

`app/src/main/AndroidManifest.xml` declares exactly one:

```
android.permission.RECORD_AUDIO
```

A tuner has to hear the note. The audio is analyzed for pitch as it arrives and is not
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
the libraries it uses and their licenses.

<!-- shared-about privacy section -->
## Who we are

Tuner is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Tuner keeps what you put in it on your phone and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Tuner is sold or shared.

Since Tuner keeps what it stores only on your phone, you can exercise most of these rights yourself: delete or share your instruments and temperaments in the app, put every setting back with Reset all settings, clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->

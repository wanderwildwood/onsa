package com.wanderwildwood.onsa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.onsa.R

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/onsa"

/**
 * Everything the APK carries that this project did not write, as the Gradle build pulls it in
 * plus the font and the music symbols it carries. Licences are the ones each project declares
 * in its own published metadata.
 */
val LICENCE_COMPONENTS: List<LicenceComponent> = listOf(
    LicenceComponent("Tuner, the app this is forked from", "Michael Moessner", "GPL-3.0-or-later", "https://codeberg.org/thetwom/Tuner"),
    LicenceComponent("Banjo tunings", "carefulcarpenter", "GPL-3.0-or-later", "https://github.com/heavencanthandle/onsa"),
    LicenceComponent("Gonville music symbols (modified)", "Simon Tatham", "Unrestricted use; source code MIT", "https://www.chiark.greenend.org.uk/~sgtatham/gonville/"),
    LicenceComponent("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
    LicenceComponent("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
    LicenceComponent("AndroidX: Core, AppCompat, Activity, Lifecycle, Navigation, DataStore, Preference, Hilt Navigation", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
    LicenceComponent("Jetpack Compose (UI, Foundation, Material 3, Animation)", "Google", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    LicenceComponent("Material Components for Android", "Google", "Apache-2.0", "https://github.com/material-components/material-components-android"),
    LicenceComponent("Dagger Hilt", "Google", "Apache-2.0", "https://dagger.dev/hilt/"),
    LicenceComponent("Accompanist Permissions", "Google", "Apache-2.0", "https://github.com/google/accompanist"),
    LicenceComponent("Kotlin standard library, coroutines, serialization, immutable collections", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
)

/** Each distinct licence text once: family id and the raw resource. */
val LICENCE_RAW: List<Pair<String, Int>> = listOf(
    "gpl" to R.raw.licence_gpl3,
    "apache" to R.raw.licence_apache2,
    "mit" to R.raw.licence_mit,
    "ofl" to R.raw.licence_ofl,
)

@Composable
fun rememberLicencesData(): LicencesData = LicencesData(
    appName = "Tuner",
    sourceUrl = SOURCE_URL,
    licenceLine = stringResource(R.string.licences_licence),
    components = LICENCE_COMPONENTS,
    texts = LICENCE_RAW.map { (id, raw) ->
        LicenceText(id, stringResource(when (id) {
            "gpl" -> R.string.licences_full_gpl
            "apache" -> R.string.licences_full_apache
            "mit" -> R.string.licences_full_mit
            "ofl" -> R.string.licences_full_ofl
            else -> R.string.licences_full_head
        }), raw)
    },
)

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    val noText = stringResource(R.string.licences_no_text, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        noText = { licence -> noText.replace("@@", licence) },
        noBrowser = { address -> none.replace("@@", address) },
    )
}

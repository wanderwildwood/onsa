package com.wanderwildwood.onsa.ui.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.onsa.BuildConfig
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.AboutData
import com.wanderwildwood.onsa.ui.AboutLabels
import com.wanderwildwood.onsa.ui.AboutLinkData
import com.wanderwildwood.onsa.ui.AboutSection
import com.wanderwildwood.onsa.ui.HouseAboutDialog
import com.wanderwildwood.onsa.ui.LicencesScreen
import com.wanderwildwood.onsa.ui.SOURCE_URL
import com.wanderwildwood.onsa.ui.rememberLicencesData
import com.wanderwildwood.onsa.ui.rememberLicencesLabels

/**
 * This app's About: only the words and the address. The dialog, its one Close button, the
 * Licences row, the links and the llama are the shared HouseAbout.kt, the same in every app.
 */
@Composable
fun AboutDialog(modifier: Modifier = Modifier, onDismiss: () -> Unit = {}) {
    val context = LocalContext.current
    val data = AboutData(
        title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
        sections = listOf(
            AboutSection(stringResource(R.string.about_privacy_title), stringResource(R.string.about_privacy_text)),
            AboutSection(stringResource(R.string.about_terms_title), stringResource(R.string.about_terms_text)),
            AboutSection(null, stringResource(R.string.about_licence)),
            AboutSection(null, stringResource(R.string.about_after)),
        ),
        sourceUrl = SOURCE_URL,
        links = listOf(
            AboutLinkData("codeberg.org/thetwom/Tuner", "https://codeberg.org/thetwom/Tuner"),
            AboutLinkData("github.com/heavencanthandle/onsa", "https://github.com/heavencanthandle/onsa"),
        ),
    )
    val labels = AboutLabels(
        close = stringResource(R.string.about_close),
        licences = stringResource(R.string.licences_title),
        feedTheLlamas = stringResource(R.string.about_feed_the_llamas),
        noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
    )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss) { modifier, back ->
        LicencesScreen(rememberLicencesData(), rememberLicencesLabels(), onBack = back, modifier = modifier)
    }
}

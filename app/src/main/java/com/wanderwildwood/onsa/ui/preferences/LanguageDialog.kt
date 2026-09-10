package com.wanderwildwood.onsa.ui.preferences

import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.LocaleListCompat
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.theme.TunerTheme

enum class LanguageSelections(val tag: String?, @StringRes val resId: Int) {
    SystemDefault(null, R.string.system_default),
    English("en", R.string.english),
    French("fr", R.string.french),
    German("de", R.string.german),
    Hungarian("hu", R.string.hungarian),
    Polish("pl", R.string.polish),
    Romanian("ro", R.string.romanian),
    Ukrainian("uk", R.string.ukrainian),
    SimplifiedChinese("zh", R.string.simplified_chinese)
}

@Composable
fun LanguageDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.abort))
            }
        },
        icon = {
            Icon(
                painterResource(R.drawable.language_24px),
                contentDescription = stringResource(R.string.language)
            )
        },
        title = {
            Text(stringResource(R.string.language))
        },
        text = {
            val selectedLocale = remember {
                LanguageSelections.entries.firstOrNull {
                    it.tag == AppCompatDelegate.getApplicationLocales()[0]?.language //toLanguageTag()
                } ?: LanguageSelections.SystemDefault
            }

            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
//                Log.v("Tuner", "Languagedialog: As is: tag=${AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()}, ${AppCompatDelegate.getApplicationLocales()[0]?.language}, ${AppCompatDelegate.getApplicationLocales()[0]?.variant}")
                LanguageSelections.entries.forEach {
                    RadioButtonLine(
                        selected = it == selectedLocale,
                        it.resId
                    ) {
                        if (it == LanguageSelections.SystemDefault) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        } else {
                            val localeList = LocaleListCompat.forLanguageTags(it.tag)
//                            Log.v("Tuner", "LanguageDialog: tag=${it.tag}, localList=${localeList[0]}")
                            AppCompatDelegate.setApplicationLocales(localeList)
                        }
                        onDismiss()
                    }
                }
            }
            //val appLocale = LocaleListCompat.forLanguageTags()
        }
    )
}

@Preview(widthDp = 300, heightDp = 500, showBackground = true)
@Composable
private fun LanguageDialogTest() {
    TunerTheme {
        LanguageDialog()
    }

}
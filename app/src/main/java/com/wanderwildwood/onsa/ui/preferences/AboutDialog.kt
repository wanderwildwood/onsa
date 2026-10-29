/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.wanderwildwood.onsa.ui.preferences

import android.content.res.Resources
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.onsa.ui.PagedColumn
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.BuildConfig
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

private const val MAIN = 0
private const val PRIVACY = 1
private const val TERMS = 2
private const val LICENCES = 3

@Composable
fun AboutDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var page by rememberSaveable { mutableIntStateOf(MAIN) }
    EInkAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButtonMMD(
                onClick = { if (page == MAIN) onDismiss() else page = MAIN },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextMMD(
                    stringResource(id = if (page == MAIN) R.string.acknowledged else R.string.about_back)
                )
            }
        },
        title = {
            TextMMD(
                when (page) {
                    PRIVACY -> stringResource(R.string.about_privacy_title)
                    TERMS -> stringResource(R.string.about_terms_title)
                    LICENCES -> stringResource(R.string.licences_title)
                    else -> stringResource(R.string.about)
                }
            )
        },
        text = {
            val resources = LocalContext.current.resources
            val licenceTexts = if (page == LICENCES) rememberLicenceTexts() else emptyList()
            // Paged, not scrolled: a page turns on whole rows, with no scrollbar.
            PagedColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                when (page) {
                    PRIVACY -> paragraphs(resources, R.string.about_privacy_text)
                    TERMS -> paragraphs(resources, R.string.about_terms_text)
                    LICENCES -> licencesItems(licenceTexts)
                    else -> {
                        item {
                            TextMMD(stringResource(id = R.string.about_message, BuildConfig.VERSION_NAME))
                        }
                        item { NavRow(stringResource(R.string.about_privacy_title)) { page = PRIVACY } }
                        item { NavRow(stringResource(R.string.about_terms_title)) { page = TERMS } }
                        item { NavRow(stringResource(R.string.licences_title)) { page = LICENCES } }
                        item {
                            Llama()
                        }
                    }
                }
            }
        },
    )
}

/** A text's paragraphs, one list row each, so a page turns on whole rows. */
private fun LazyListScope.paragraphs(resources: Resources, text: Int) {
    resources.getString(text).split("\n\n").forEach { paragraph ->
        item {
            TextMMD(paragraph, modifier = Modifier.padding(bottom = 8.dp))
        }
    }
}

/** A row that opens a page of its own: 48 dp tall to press, with an arrow at the end. */
@Composable
private fun NavRow(text: String, onPress: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onPress),
    ) {
        TextMMD(text, style = MaterialTheme.typography.bodyLarge)
        TextMMD("\u203A", style = MaterialTheme.typography.bodyLarge)
    }
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun AboutDialogTest() {
    TunerTheme {
        AboutDialog()
    }
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to.
 * It shares one line with the site's name, which opens the site; the llama and its
 * words open the page.
 *
 * Three words rather than an address: a verb and an object, so what happens when you press
 * them is not a surprise even though the page is not named. The drawing is his own, and it is
 * ink rather than an emoji, which is a colour glyph and reaches the panel as a pale smudge.
 *
 * The Kompakt may have nothing registered for a web address, so the intent is allowed to fail
 * quietly rather than take the dialog down with it.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            // The site's address opens the site, the way the llama beside it opens its page.
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wanderthe.dev")),
                        )
                    }.onFailure {
                        Toast.makeText(context, context.getString(R.string.about_no_browser), Toast.LENGTH_SHORT).show()
                    }
                }
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
        ) {
            TextMMD("wanderthe.dev")
        }
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    // Straight to the checkout. The Donate button on the site only leads
                    // here anyway, so the page in between is a press the reader does not need.
                    // The short square.link form, not the long checkout.square.site address it
                    // redirects to -- the short one is what the site itself links to, so a
                    // regenerated checkout follows it and a published app does not break.
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://square.link/u/AGu8oT10")),
                        )
                    }.onFailure {
                        Toast.makeText(context, context.getString(R.string.about_no_browser), Toast.LENGTH_SHORT).show()
                    }
                }
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(stringResource(R.string.about_feed_the_llamas))
        }
    }
}

package com.wanderwildwood.onsa.ui.instruments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.instruments.Instrument
import com.wanderwildwood.onsa.instruments.InstrumentGroup
import com.wanderwildwood.onsa.ui.common.EditableListItem
import com.wanderwildwood.onsa.ui.common.ListItemTask
import com.wanderwildwood.onsa.ui.misc.TunerScaffoldWithoutBottomBar
import com.wanderwildwood.onsa.ui.notes.NotePrintOptions2
import kotlinx.collections.immutable.ImmutableList

/**
 * One instrument's tunings: the built-in ones first, in their usual order, then the reader's.
 *
 * Opened from the instrument's row in the instrument list. Tapping a tuning tunes to it. The
 * reader's own tunings can be edited, copied and deleted from here, the built-in ones copied,
 * and the + starts a new tuning already filed under this instrument.
 */
@Composable
fun InstrumentGroupPage(
    group: InstrumentGroup,
    tunings: ImmutableList<Instrument>,
    activeInstrument: Instrument?,
    modifier: Modifier = Modifier,
    notePrintOptions: NotePrintOptions2 = NotePrintOptions2(),
    onTuningClicked: (tuning: Instrument) -> Unit = {},
    onEditTuningClicked: (tuning: Instrument, copy: Boolean) -> Unit = { _, _ -> },
    onDeleteTuningClicked: (tuning: Instrument) -> Unit = {},
    onCreateNewTuningClicked: () -> Unit = {},
    onNavigateUpClicked: () -> Unit = {},
    snackbarHostState: SnackbarHostStateMMD = remember { SnackbarHostStateMMD() }
) {
    val context = LocalContext.current
    val maxExpectedHeightForFab = 72.dp

    TunerScaffoldWithoutBottomBar(
        modifier = modifier,
        title = stringResource(id = group.nameResource),
        onNavigateUpClicked = onNavigateUpClicked,
        showPreferenceButton = false,
        floatingActionButton = {
            FloatingActionButtonMMD(onClick = onCreateNewTuningClicked) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = stringResource(R.string.cd_create_new_instrument)
                )
            }
        },
        snackbarHost = { SnackbarHostMMD(hostState = snackbarHostState) }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumnMMD(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + maxExpectedHeightForFab
            ),
            isScrollbarVisible = false,
        ) {
            items(tunings, key = { it.stableId }) { tuning ->
                EditableListItem(
                    title = {
                        TextMMD(
                            tuning.getTuningNameString(context),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    description = {
                        val style = LocalTextStyle.current
                        val stringsString = remember(context, style, tuning, notePrintOptions) {
                            tuning.getStringsString(
                                context = context,
                                notePrintOptions = notePrintOptions,
                                fontSize = style.fontSize,
                                fontWeight = style.fontWeight
                            )
                        }
                        TextMMD(stringsString, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    icon = {
                        Icon(
                            ImageVector.vectorResource(id = tuning.icon.resourceId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    modifier = Modifier.clickable { onTuningClicked(tuning) },
                    onOptionsClicked = {
                        when (it) {
                            ListItemTask.Edit -> onEditTuningClicked(tuning, false)
                            ListItemTask.Copy -> onEditTuningClicked(tuning, true)
                            ListItemTask.Delete -> onDeleteTuningClicked(tuning)
                            ListItemTask.Info -> {}
                        }
                    },
                    isActive = activeInstrument?.stableId == tuning.stableId,
                    readOnly = tuning.isPredefined(),
                    isCopyable = true,
                    hasInfo = false
                )
            }
        }
    }
}

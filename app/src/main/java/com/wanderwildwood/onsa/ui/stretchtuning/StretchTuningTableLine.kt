package com.wanderwildwood.onsa.ui.stretchtuning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.ui.common.OverflowMenuCallbacks
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

/** Get the width of a table column.
 * @param testString Test string based on which we compute the width. This should e.g. be the
 *   longest expected string of the table column.
 * @param style Text style of content.
 * @param density Text density of content.
 * @param textMeasurer Text measurer which does the measuring.
 * @return Width of table column.
 */
@Composable
fun rememberTableColumnWidth(
    testString: String,
    style: TextStyle = LocalTextStyle.current,
    density: Density = LocalDensity.current,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): Dp {
    return remember(textMeasurer, density, style) {
        with(density) {
            textMeasurer.measure(testString, style = style, density = density).size.width.toDp()
        }
    }
}

@Composable
private fun OverflowMenu(
    onDeleteClicked: () -> Unit,
    onEditClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = {
            expanded = !expanded
        }) {
            Icon(
                painter = painterResource(R.drawable.more_vert_24px),
                contentDescription = "menu"
            )
        }
        DropdownMenuMMD(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(id = R.string.edit)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.edit_24px),
                        contentDescription = "edit"
                    )
                },
                onClick = {
                    onEditClicked()
                    expanded = false
                }
            )
            HorizontalDividerMMD()
            DropdownMenuItemMMD(
                text = { TextMMD(stringResource(id = R.string.delete)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.delete_24px),
                        contentDescription = "delete"
                    )
                },
                onClick = {
                    onDeleteClicked()
                    expanded = false
                }
            )
        }
   }
}

/** Line of stretch tuning table.
 * @param column1 Content of first column.
 * @param column2 Content of second column.
 * @param column1Width Minimum column width of first column. Can be computed with
 *   with `rememberTableColumnWidth()`.
 *  @param column2Width Minimum column width of second column. Can be computed with
 *   with `rememberTableColumnWidth()`.
 * @param modifier Modifier.
 * @param isSelected If true, the line will be highlighted.
 * @param textStyle Text style used.
 * @param textAlign How to align the text in the columns.
 * @param showOverflowMenu True to show the overflow menu at the end.
 * @param reserveSpaceForOverflowMenu Whether we should reserve space for the overflow menu, when
 *    it is not shown. So when you have several lines, where some show an overflow menu and some not
 *    you might want to still reserve space for the lines without overflow menu, such that different
 *    lines are aligned.
 */
@Composable
fun StretchTuningTableLine(
    column1: String,
    column2: String,
    column1Width: Dp,
    column2Width: Dp,
    modifier: Modifier = Modifier,
    onEditClicked: () -> Unit = {},
    onDeleteClicked: () -> Unit = {},
    isSelected: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign = TextAlign.End,
    showOverflowMenu: Boolean = true,
    reserveSpaceForOverflowMenu: Boolean = true,
    errorMessage: String? = null
) {
    Surface(
        modifier = modifier,
        color = if (errorMessage != null) {
            MaterialTheme.colorScheme.errorContainer
        } else if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Spacer(modifier = Modifier.weight(2f))
                TextMMD(
                    column1,
                    modifier = Modifier.padding(start=16.dp).width(column1Width),
                    maxLines = 1,
                    style = textStyle,
                    textAlign = textAlign
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(32.dp))
                Spacer(modifier = Modifier.weight(1f))
                TextMMD(
                    column2,
                    modifier = Modifier.padding(end=16.dp).width(column2Width),
                    style = textStyle,
                    textAlign = textAlign
                )
                Spacer(modifier = Modifier.weight(2f))
                if (showOverflowMenu) {
                    OverflowMenu(
                        onEditClicked = onEditClicked,
                        onDeleteClicked = onDeleteClicked,
                        modifier = Modifier.size(48.dp)
                    )
                } else if (reserveSpaceForOverflowMenu) {
                    Spacer(Modifier.size(48.dp))
                }
            }

            if (errorMessage != null) {
                TextMMD(
                    errorMessage,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDividerMMD()
        }
    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun  StretchTuningTableLinePreview() {
    TunerTheme {
        Surface {
            Box {
                Column {
                    val stretchTuningData = remember {
                        StretchTuning(
                            name = GetTextFromString("Test"),
                            description = GetTextFromString("Stretch tuning description."),
                            unstretchedFrequencies = doubleArrayOf(320.0, 440.0, 540.0),
                            stretchInCents = doubleArrayOf(-3.0, 0.0, 4.0)
                        )
                    }
                    val style = MaterialTheme.typography.bodyLarge
                    val frequencyWidth = rememberTableColumnWidth(
                        stringResource(R.string.hertz_2f, 10000f),
                        style = style
                    )
                    val centCorrectionWidth = rememberTableColumnWidth(
                        stringResource(R.string.cent_2f, 10f),
                        style = style
                    )

                    stretchTuningData.unstretchedFrequencies.forEachIndexed { index, unstretchedFrequency ->
                        StretchTuningTableLine(
                            column1 = stringResource(R.string.hertz_1f, unstretchedFrequency),
                            column2 = stringResource(R.string.cent_1f, stretchTuningData.stretchInCents[index]),
                            isSelected = (index == 1),
                            column1Width = frequencyWidth,
                            column2Width = centCorrectionWidth,
                            textStyle = style,
                            errorMessage = if (index == 1) "This is an error message, and it is very long, to show what happens when the line breaks" else null
                        )
                    }
                }
            }
        }
    }
}
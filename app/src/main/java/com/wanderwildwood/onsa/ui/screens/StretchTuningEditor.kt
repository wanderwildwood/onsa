package com.wanderwildwood.onsa.ui.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.menus.DropdownMenuItemMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.temperaments.centsToRatio
import com.wanderwildwood.onsa.ui.plot.GestureBasedViewPort
import com.wanderwildwood.onsa.ui.stretchtuning.StretchTuningGraph
import com.wanderwildwood.onsa.ui.stretchtuning.StretchTuningTable
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface StretchTuningEditorState {

    val name: State<String>
    val description: State<String>
    val stretchTuning: StateFlow<StretchTuning>
    val selectedKey: StateFlow<Int>
    val gestureBasedViewPort: GestureBasedViewPort
    val lazyListState: LazyListState

    fun modifyName(name: String)

    fun modifyDescription(description: String)

    fun select(key: Int)
    fun addLine()
    fun removeLine(key: Int)
    fun modifyLine(unstretchedFrequency: Double, stretchInCents: Double, key: Int)
}

//@Composable
//private fun OverflowMenu(
//    onLoadClicked: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    var expanded by rememberSaveable { mutableStateOf(false) }
//    Box(modifier = modifier) {
//        IconButton(onClick = {
//            expanded = !expanded
//        }) {
//            Icon(Icons.Default.MoreVert, contentDescription = "menu")
//        }
//        DropdownMenuMMD(
//            expanded = expanded,
//            onDismissRequest = { expanded = false }
//        ) {
//            DropdownMenuItemMMD(
//                text = { TextMMD(stringResource(id = R.string.edit)) },
//                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "load") },
//                onClick = {
//                    onLoadClicked()
//                    expanded = false
//                }
//            )
//            // HorizontalDividerMMD()
//        }
//    }
//}

@Composable
fun StretchTuningEditor(
    state:  StretchTuningEditorState,
    modifier: Modifier = Modifier,
    onEditLineClicked: ( key: Int ) -> Unit = {},
    onAbortClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {},
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            StretchTuningLandscape(
                state, modifier, onEditLineClicked, onAbortClicked, onSaveClicked,
                tunerPlotStyle
            )
        }

        else -> {
            StretchTuningPortrait(
                state, modifier, onEditLineClicked, onAbortClicked, onSaveClicked,
                tunerPlotStyle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchTuningPortrait(
    state: StretchTuningEditorState,
    modifier: Modifier,
    onEditLineClicked: ( key: Int ) -> Unit,
    onAbortClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    tunerPlotStyle: TunerPlotStyle
) {
    val stretchTuningData by state.stretchTuning.collectAsStateWithLifecycle()
    val selectedKey by state.selectedKey.collectAsStateWithLifecycle()

    //TunerScaffoldWithoutBottomBar(
    //    title = stringResource(R.string.stretch_tuning),
    //    showPreferenceButton = false,
    //    //defaultModeTools = { OverflowMenu(onLoadClicked = {}) },
    //    floatingActionButton = {
    //        FloatingActionButtonMMD(
    //            onClick = {
    //                state.addLine()
    //            }
    //        ) {
    //            Icon(Icons.Default.Add, contentDescription = "add")
    //        }
    //    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(stringResource(id = R.string.stretch_tuning_editor)) },
                navigationIcon = {
                    IconButton(onClick = { onAbortClicked() }) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            stringResource(R.string.cd_close)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSaveClicked() },
                        enabled = stretchTuningData.isMonotonic
                    ) {
                        TextMMD(stringResource(id = R.string.save))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButtonMMD(
                onClick = {
                    state.addLine()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = stringResource(R.string.cd_add)
                )
            }
        }
    ){ paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = modifier
                .consumeWindowInsets(paddingValues)
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = tunerPlotStyle.margin + paddingValues.calculateStartPadding(layoutDirection),
                    end = tunerPlotStyle.margin + paddingValues.calculateEndPadding(layoutDirection)
                )
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextFieldMMD(
                value = state.name.value,
                onValueChange = { state.modifyName(it) },
                label = { TextMMD(stringResource(id = R.string.name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                trailingIcon = {
                    IconButton(onClick = { state.modifyName("") }) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = stringResource(R.string.cd_clear_text)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            TextFieldMMD(
                value = state.description.value,
                onValueChange = { state.modifyDescription(it) },
                label = { TextMMD(stringResource(id = R.string.description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = tunerPlotStyle.margin
                    ),
                trailingIcon = {
                    IconButton(onClick = { state.modifyDescription("") }) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = stringResource(R.string.cd_clear_text)
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            StretchTuningGraph(
                stretchTuningData,
                selectedKey = selectedKey,
                gestureBasedViewPort = state.gestureBasedViewPort,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                lineWidth = tunerPlotStyle.plotLineWidth,
                lineColor = tunerPlotStyle.plotLineColor,
                tickLineWidth = tunerPlotStyle.tickLineWidth,
                tickLineColor = tunerPlotStyle.tickLineColor,
                tickLabelStyle = tunerPlotStyle.tickFontStyle,
                plotWindowOutline = if (state.gestureBasedViewPort.isActive)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline
            )
            StretchTuningTable(
                stretchTuningData,
                selectedKey = selectedKey,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 76.dp // additional padding to get above the fab button
                ),
                onLineClicked = { state.select(it) },
                onEditLineClicked = { onEditLineClicked(it) },
                onDeleteLineClicked = { state.removeLine(it) },
                lazyListState = state.lazyListState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchTuningLandscape(
    state: StretchTuningEditorState,
    modifier: Modifier,
    onEditLineClicked: ( key: Int ) -> Unit,
    onAbortClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    tunerPlotStyle: TunerPlotStyle
) {
    val stretchTuningData by state.stretchTuning.collectAsStateWithLifecycle()
    val selectedKey by state.selectedKey.collectAsStateWithLifecycle()

    // TunerScaffoldWithoutBottomBar(
    //     title = stringResource(R.string.stretch_tuning),
    //     showPreferenceButton = false,
    //     //defaultModeTools = { OverflowMenu(onLoadClicked = {}) },
    //     floatingActionButton = {
    //         FloatingActionButtonMMD(
    //             onClick = {
    //                 state.addLine()
    //             }
    //         ) {
    //             Icon(Icons.Default.Add, contentDescription = "add")
    //         }
    //     }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(stringResource(id = R.string.stretch_tuning_editor)) },
                navigationIcon = {
                    IconButton(onClick = { onAbortClicked() }) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            stringResource(R.string.cd_close)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSaveClicked() },
                        enabled = stretchTuningData.isMonotonic
                    ) {
                        TextMMD(stringResource(id = R.string.save))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButtonMMD(
                onClick = {
                    state.addLine()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_24px),
                    contentDescription = stringResource(R.string.cd_add)
                )
            }
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current

        Row(
            modifier = modifier
                .consumeWindowInsets(paddingValues)
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = tunerPlotStyle.margin + paddingValues.calculateStartPadding(layoutDirection),
                    end = tunerPlotStyle.margin + paddingValues.calculateEndPadding(layoutDirection)
                    ),
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        end = tunerPlotStyle.margin / 2,
                        bottom = paddingValues.calculateBottomPadding()
                    )
                    .weight(1f)
            ) {
                TextFieldMMD(
                    value = state.name.value,
                    onValueChange = { state.modifyName(it) },
                    label = { TextMMD(stringResource(id = R.string.name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = tunerPlotStyle.margin),
                    trailingIcon = {
                        IconButton(onClick = { state.modifyName("") }) {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = stringResource(R.string.cd_clear_text)
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                StretchTuningGraph(
                    stretchTuningData,
                    selectedKey = selectedKey,
                    gestureBasedViewPort = state.gestureBasedViewPort,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                            bottom = 16.dp
                        ),
                    lineWidth = tunerPlotStyle.plotLineWidth,
                    lineColor = tunerPlotStyle.plotLineColor,
                    tickLineWidth = tunerPlotStyle.tickLineWidth,
                    tickLineColor = tunerPlotStyle.tickLineColor,
                    tickLabelStyle = tunerPlotStyle.tickFontStyle,
                    plotWindowOutline = if (state.gestureBasedViewPort.isActive)
                        tunerPlotStyle.plotWindowOutlineDuringGesture
                    else
                        tunerPlotStyle.plotWindowOutline
                )
            }

            Column(
                modifier = Modifier
                    .padding(start = tunerPlotStyle.margin / 2)
                    .weight(1f)
            ) {
                TextFieldMMD(
                    value = state.description.value,
                    onValueChange = { state.modifyDescription(it) },
                    label = { TextMMD(stringResource(id = R.string.description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    trailingIcon = {
                        IconButton(onClick = { state.modifyDescription("") }) {
                            Icon(
                                painter = painterResource(R.drawable.close_24px),
                                contentDescription = stringResource(R.string.cd_clear_text)
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                StretchTuningTable(
                    stretchTuningData,
                    selectedKey = selectedKey,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = paddingValues.calculateBottomPadding() + 76.dp // additional padding to get above the fab button
                    ),
                    onLineClicked = { state.select(it) },
                    onEditLineClicked = { onEditLineClicked(it) },
                    onDeleteLineClicked = { state.removeLine(it) },
                    lazyListState = state.lazyListState
                )
            }
        }
    }
}

private class TestStretchTuningState : StretchTuningEditorState {

    override val name = mutableStateOf("some name")

    override val description = mutableStateOf("stretch tuning description")
    override val stretchTuning = MutableStateFlow(StretchTuning(
        name = GetTextFromString("Test"),
        description = GetTextFromString("Stretch tuning description."),
        unstretchedFrequencies = doubleArrayOf(320.0, 440.0, 540.0, 1023.4, 3000.0, 12310.0),
        stretchInCents = doubleArrayOf(-4.0, 0.9, 1.2, 2.4, 5.0, 7.6)
    ))
    override val selectedKey = MutableStateFlow(2)

    override val gestureBasedViewPort = GestureBasedViewPort()

    override val lazyListState = LazyListState()

    override fun modifyName(name: String) {
        this.name.value = name
    }

    override fun modifyDescription(description: String) {
        this.description.value = description
    }
    override fun select(key: Int) {
        selectedKey.value = key
    }

    override fun addLine() {
        stretchTuning.value = stretchTuning.value.add(
            stretchTuning.value.unstretchedFrequencies[0] - 10,
            stretchTuning.value.stretchInCents[0] - 1
        )
    }

    override fun removeLine(key: Int) {
        stretchTuning.value = stretchTuning.value.remove(key)
    }

    override fun modifyLine(unstretchedFrequency: Double, stretchInCents: Double, key: Int) {
        // Log.v("Tuner", "StretchTuningEditor: modifyLine: unstretched frequency = $unstretchedFrequency, stretchInCents = $stretchInCents")
        stretchTuning.value = stretchTuning.value.modify(unstretchedFrequency, stretchInCents, key)
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Preview(widthDp = 800, heightDp = 400)
@Composable
fun StretchTuningEditorPreview() {
    TunerTheme {
        val state = remember { TestStretchTuningState() }

        Surface() {
            StretchTuningEditor(state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
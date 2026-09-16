package com.wanderwildwood.onsa.ui.stretchtuning

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.ui.plot.GestureBasedViewPort
import com.wanderwildwood.onsa.ui.plot.rememberGestureBasedViewPort
import com.wanderwildwood.onsa.ui.screens.TunerPlotStyle
import com.wanderwildwood.onsa.ui.theme.TunerTheme

@Composable
fun StretchTuningInfo(
    stretchTuning: StretchTuning,
    modifier: Modifier = Modifier,
    onNavigateUpClicked: () -> Unit = {},
    tunerPlotStyle: TunerPlotStyle = TunerPlotStyle.create()
) {
    val configuration = LocalConfiguration.current
    val gestureBasedViewPort = rememberGestureBasedViewPort()
    val lazyListState = rememberLazyListState()
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            StretchTuningInfoLandscape(
                stretchTuning, modifier, onNavigateUpClicked,
                gestureBasedViewPort, lazyListState,
                tunerPlotStyle
            )
        }

        else -> {
            StretchTuningInfoPortrait(
                stretchTuning, modifier, onNavigateUpClicked,
                gestureBasedViewPort, lazyListState,
                tunerPlotStyle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchTuningInfoPortrait(
    stretchTuning: StretchTuning,
    modifier: Modifier,
    onNavigateUpClicked: () -> Unit,
    gestureBasedViewPort: GestureBasedViewPort,
    lazyListState: LazyListState,
    tunerPlotStyle: TunerPlotStyle
) {
    var selectedKey by rememberSaveable { mutableIntStateOf(StretchTuning.NO_KEY) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(stringResource(id = R.string.stretch_tuning_info)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateUpClicked() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            "back"
                        )
                    }
                },
            )
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
            val context = LocalContext.current
            TextMMD(
                stretchTuning.name.value(context),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                ,
                style = MaterialTheme.typography.titleLarge
            )
            TextMMD(
                stretchTuning.description.value(context),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = tunerPlotStyle.margin, start = 8.dp, end = 8.dp)
            )
            StretchTuningGraph(
                stretchTuning,
                selectedKey = selectedKey,
                gestureBasedViewPort = gestureBasedViewPort,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f),
                lineWidth = tunerPlotStyle.plotLineWidth,
                lineColor = tunerPlotStyle.plotLineColor,
                tickLineWidth = tunerPlotStyle.tickLineWidth,
                tickLineColor = tunerPlotStyle.tickLineColor,
                tickLabelStyle = tunerPlotStyle.tickFontStyle,
                plotWindowOutline = if (gestureBasedViewPort.isActive)
                    tunerPlotStyle.plotWindowOutlineDuringGesture
                else
                    tunerPlotStyle.plotWindowOutline

            )
            StretchTuningTable(
                stretchTuning,
                selectedKey = selectedKey,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    bottom = paddingValues.calculateBottomPadding() + 76.dp // additional padding to get above the fab button
                ),
                onLineClicked = { selectedKey = it },
                lazyListState = lazyListState,
                showOverflowMenu = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StretchTuningInfoLandscape(
    stretchTuning: StretchTuning,
    modifier: Modifier,
    onNavigateUpClicked: () -> Unit,
    gestureBasedViewPort: GestureBasedViewPort,
    lazyListState: LazyListState,
    tunerPlotStyle: TunerPlotStyle
) {
    var selectedKey by rememberSaveable { mutableIntStateOf(StretchTuning.NO_KEY) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(stringResource(id = R.string.stretch_tuning_info)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUpClicked) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            "back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current

        Column(
            modifier = modifier
                .consumeWindowInsets(paddingValues)
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = tunerPlotStyle.margin + paddingValues.calculateStartPadding(layoutDirection),
                    end = tunerPlotStyle.margin + paddingValues.calculateEndPadding(layoutDirection)
                ),
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val context = LocalContext.current
                TextMMD(
                    stretchTuning.name.value(context),
                    modifier = Modifier.padding(start = 8.dp, end = 20.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                TextMMD(
                    stretchTuning.description.value(context)
                )
            }
            Row {
                StretchTuningGraph(
                    stretchTuning,
                    selectedKey = selectedKey,
                    gestureBasedViewPort = gestureBasedViewPort,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(
                            top = tunerPlotStyle.margin,
                            end = tunerPlotStyle.margin / 2,
                            bottom = tunerPlotStyle.margin + paddingValues.calculateBottomPadding()
                        ),
                    lineWidth = tunerPlotStyle.plotLineWidth,
                    lineColor = tunerPlotStyle.plotLineColor,
                    tickLineWidth = tunerPlotStyle.tickLineWidth,
                    tickLineColor = tunerPlotStyle.tickLineColor,
                    tickLabelStyle = tunerPlotStyle.tickFontStyle,
                    plotWindowOutline = if (gestureBasedViewPort.isActive)
                        tunerPlotStyle.plotWindowOutlineDuringGesture
                    else
                        tunerPlotStyle.plotWindowOutline
                )

                StretchTuningTable(
                    stretchTuning,
                    selectedKey = selectedKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        bottom = paddingValues.calculateBottomPadding() + 76.dp // additional padding to get above the fab button
                    ),
                    onLineClicked = { selectedKey = it },
                    lazyListState = lazyListState,
                    showOverflowMenu = false
                )
            }
        }
    }
}


@Preview(widthDp = 400, heightDp = 800)
@Preview(widthDp = 800, heightDp = 400)
@Composable
fun StretchTuningEditorPreview() {
    TunerTheme {
        val stretchTuning = remember {
            StretchTuning(
                name = GetTextFromString("Test"),
                description = GetTextFromString("Stretch tuning description."),
                unstretchedFrequencies = doubleArrayOf(320.0, 440.0, 540.0, 1023.4, 3000.0, 12310.0),
                stretchInCents = doubleArrayOf(-3.0, 0.0, 1.0, 5.0, 8.0, 12.3)
            )
        }

        Surface() {
            StretchTuningInfo(
                stretchTuning,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
package com.wanderwildwood.onsa.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mudita.mmd.components.buttons.FloatingActionButtonMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.misc.GetTextFromString
import com.wanderwildwood.onsa.misc.ShareData
import com.wanderwildwood.onsa.misc.getFilenameFromUri
import com.wanderwildwood.onsa.misc.toastPotentialFileCheckError
import com.wanderwildwood.onsa.stretchtuning.StretchTuning
import com.wanderwildwood.onsa.stretchtuning.StretchTuningIO
import com.wanderwildwood.onsa.ui.common.EditableList
import com.wanderwildwood.onsa.ui.common.EditableListData
import com.wanderwildwood.onsa.ui.common.EditableListItem
import com.wanderwildwood.onsa.ui.common.ListItemTask
import com.wanderwildwood.onsa.ui.common.OverflowMenu
import com.wanderwildwood.onsa.ui.common.OverflowMenuCallbacks
import com.wanderwildwood.onsa.ui.misc.TunerScaffoldWithoutBottomBar
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface StretchTuningOverviewState {
    val defaultStretchTuning: StretchTuning
    val listData: EditableListData<StretchTuning>
    fun saveStretchTunings(context: Context, uri: Uri, stretchTunings: List<StretchTuning>)
}

@Composable
private fun rememberImportExportCallbacks(
    state: StretchTuningOverviewState,
    onLoadStretchTunings: (stretchTunings: List<StretchTuning>) -> Unit
): OverflowMenuCallbacks {
    val context = LocalContext.current
    val resources = LocalResources.current
    val stateUpdated by rememberUpdatedState(newValue = state)
    val onLoadStretchTuningsUpdated by rememberUpdatedState(newValue = onLoadStretchTunings)

    val saveStretchTuningsLauncher =  rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            val stretchTunings = stateUpdated.listData.extractSelectedItems()
            stateUpdated.saveStretchTunings(
                context, uri, stretchTunings
            )
            stateUpdated.listData.clearSelectedItems()
            val filename = getFilenameFromUri(context, uri)
            Toast.makeText(
                context,
                resources.getQuantityString(
                    R.plurals.database_num_saved,
                    stretchTunings.size,
                    stretchTunings.size,
                    filename
                ),
                Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context,
                R.string.failed_to_archive_items, Toast.LENGTH_LONG).show()
        }
    }

    val shareStretchTuningsLauncher = rememberLauncherForActivityResult(
        contract = ShareData.Contract()
    ) {
        state.listData.clearSelectedItems()
    }

    val importStretchTuningsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
//            val cR = context.contentResolver
//            Log.v("Tuner", "Temperaments import temperament file, mimetype=${cR.getType(uri)}")

            val (readState, stretchTunings) = StretchTuningIO.readStretchTuningsFromFile(context, uri)
            readState.toastPotentialFileCheckError(context, uri)
            if (stretchTunings.isNotEmpty()) {
                onLoadStretchTuningsUpdated(stretchTunings)
                state.listData.clearSelectedItems()
            }
        }
    }
    return remember(context) {
        object: OverflowMenuCallbacks {
            override fun onDeleteClicked() {
                if (stateUpdated.listData.selectedItems.value.isNotEmpty())
                    stateUpdated.listData.deleteSelectedItems()
                else
                    stateUpdated.listData.deleteAllItems()
            }
            override fun onShareClicked() {
                val stretchTunings = stateUpdated.listData.extractSelectedItems()
                if (stretchTunings.isEmpty()) {
                    Toast.makeText(context, R.string.database_empty_share, Toast.LENGTH_LONG).show()
                } else {
                    val intent = ShareData.createShareDataIntent(
                        context,
                        "tuner-stretch-tunings.txt",
                        StretchTuningIO.stretchTuningsToString(context, stretchTunings),
                        stretchTunings.size
                    )
                    shareStretchTuningsLauncher.launch(intent)
                }
            }
            override fun onExportClicked() {
                if (stateUpdated.listData.editableItems.value.isEmpty()) {
                    Toast.makeText(context, R.string.database_empty, Toast.LENGTH_LONG).show()
                } else {
                    saveStretchTuningsLauncher.launch("stretch-tunings.txt")
                }
            }
            override fun onImportClicked() {
                importStretchTuningsLauncher.launch(arrayOf("text/plain", "application/octet-stream"))  // text/plain or */*
            }
            override fun onSettingsClicked() {
                // onPreferenceButtonClicked()
            }
        }
    }
}

@Composable
fun StretchTuningOverview(
    state: StretchTuningOverviewState,
    modifier: Modifier = Modifier,
    canNavigateUp: Boolean = true,
    onNavigateUp: () -> Unit = {},
    onStretchTuningClicked: (StretchTuning) -> Unit = {},
    onEditStretchTuningClicked: (StretchTuning, copy: Boolean) -> Unit = {_,_->},
    onStretchTuningInfoClicked: (StretchTuning) -> Unit = {},
    onLoadStretchTunings: (List<StretchTuning>) -> Unit = {}
) {
    val context = LocalContext.current
    val selectedStretchTunings by state.listData.selectedItems.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostStateMMD() }
    val maxExpectedHeightForFab = 72.dp

    val overflowCallbacks = rememberImportExportCallbacks(
        state = state,
        onLoadStretchTunings = onLoadStretchTunings
    )

    TunerScaffoldWithoutBottomBar(
        modifier = modifier,
        title = stringResource(id = R.string.stretch_tuning),
        defaultModeTools = {
            OverflowMenu(
                callbacks = overflowCallbacks,
                showSettings = false
            )
        },
        actionModeActive = selectedStretchTunings.isNotEmpty(),
        actionModeTitle = "${selectedStretchTunings.size}",
        actionModeTools = {
            IconButton(onClick = {
                scope.launch {
                    val changed = state.listData.moveSelectedItemsUp()
                    if (changed) {
                        listState.animateScrollToItem(
                            (listState.firstVisibleItemIndex - 1).coerceAtLeast(0),
                            -listState.firstVisibleItemScrollOffset
                        )
                    }
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.keyboard_arrow_up_24px),
                    contentDescription = "move up"
                )
            }
            IconButton(onClick = {
                scope.launch {
                    val changed = state.listData.moveSelectedItemsDown()
                    if (changed) {
                        listState.animateScrollToItem(
                            listState.firstVisibleItemIndex + 1,
                            -listState.firstVisibleItemScrollOffset
                        )
                    }
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.keyboard_arrow_down_24px),
                    contentDescription = "move down"
                )
            }
           OverflowMenu(overflowCallbacks, showSettings = false)
        },
        onActionModeFinishedClicked = {
            state.listData.clearSelectedItems()
        },
        canNavigateUp = canNavigateUp,
        onNavigateUpClicked = onNavigateUp,
        showPreferenceButton = false,
        floatingActionButton = {
            Row {
                ExtendedFloatingActionButton(
                    onClick = {
                        onStretchTuningClicked(state.defaultStretchTuning)
                    },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                    icon = {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_reset),
                            contentDescription = "reset"
                        )
                    },
                    text = { TextMMD(stringResource(R.string.use_default)) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FloatingActionButtonMMD(
                    onClick = {
                        onEditStretchTuningClicked(
                            StretchTuning(
                                name = GetTextFromString(""),
                                description = GetTextFromString(""),
                                unstretchedFrequencies = doubleArrayOf(),
                                stretchInCents = doubleArrayOf()
                            ),
                            true
                        )
                    },
                    containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_24px),
                        contentDescription = "new stretch tuning"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHostMMD(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        val iconTextSize = with(LocalDensity.current) { 18.dp.toSp() }
        val layoutDirection = LocalLayoutDirection.current
        EditableList(
            state = state.listData,
            modifier = modifier.consumeWindowInsets(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + maxExpectedHeightForFab
            ),
            onActivateItemClicked = { onStretchTuningClicked(it) },
            snackbarHostState = snackbarHostState
       ) { item, itemInfo, itemModifier ->
            EditableListItem(
                title = { TextMMD(
                    item.name.value(context),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) },
                description = { TextMMD(
                    item.description.value(context),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                ) },
                icon = {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LocalContentColor.current)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Log.v("Tuner", "StretchTuningOverview: name=${item.name.value(context)}, 0=${item.name.value(context).getOrNull(0)?.code}")
                            TextMMD(
                                text = "${item.name.value(context).getOrNull(0) ?: ""}",
                                fontSize = iconTextSize
                            )
                        }
                    }
                },
                modifier = itemModifier,
                onOptionsClicked = {
                    when (it) {
                        ListItemTask.Edit -> onEditStretchTuningClicked(item, false)
                        ListItemTask.Copy -> onEditStretchTuningClicked(item, true)
                        ListItemTask.Delete -> {
                            state.listData.deleteItems(persistentSetOf(item.stableId))
                        }

                        ListItemTask.Info -> onStretchTuningInfoClicked(item)
                    }
                },
                isActive = itemInfo.isActive,
                isSelected = itemInfo.isSelected,
                readOnly = itemInfo.readOnly,
                isCopyable = true,
                hasInfo = true
            )
        }
    }
}


private class StretchTuningOverviewTestState : StretchTuningOverviewState {

    private val editableItems = MutableStateFlow(
        persistentListOf(
            StretchTuning(
                name = GetTextFromString("Stretch 1"),
                description = GetTextFromString("Description stretch 1"),
                unstretchedFrequencies = doubleArrayOf(100.0, 400.0, 600.0),
                stretchInCents = doubleArrayOf(-3.0, 1.0, 2.0),
                stableId = 1L
            ),
            StretchTuning(
                name = GetTextFromString("Stretch 2"),
                description = GetTextFromString("Description stretch 2"),
                unstretchedFrequencies = doubleArrayOf(100.0, 400.0, 600.0),
                stretchInCents = doubleArrayOf(-2.0, 1.0, 2.0),
                stableId = 2L
            ),
            StretchTuning(
                name = GetTextFromString("Stretch 3"),
                description = GetTextFromString("Description stretch 3"),
                unstretchedFrequencies = doubleArrayOf(100.0, 400.0, 600.0),
                stretchInCents = doubleArrayOf(-5.0, 4.0, 9.0),
                stableId = 3L
            )
        )
    )

    override val defaultStretchTuning get() = editableItems.value[0]

    private val editableItemsExpanded = MutableStateFlow(true)

    private val activeItem = MutableStateFlow(editableItems.value[1])

    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(),
        editableItemsSectionResId = R.string.custom_item,
        editableItems = editableItems,
        editableItemsExpanded = editableItemsExpanded,
        toggleEditableItemsExpanded = { editableItemsExpanded.value = !editableItemsExpanded.value },
        getStableId = { it.stableId },
        activeItem = activeItem,
        setNewItems = { editableItems.value = it }
    )

    override fun saveStretchTunings(
        context: Context,
        uri: Uri,
        stretchTunings: List<StretchTuning>
    ) {

    }

}

@Preview(heightDp = 800, widthDp = 400, showBackground = true)
@Composable
private fun StretchTuningOverviewPreview() {
    TunerTheme {
        val state = remember { StretchTuningOverviewTestState() }
        StretchTuningOverview(
            state = state
        )
    }
}
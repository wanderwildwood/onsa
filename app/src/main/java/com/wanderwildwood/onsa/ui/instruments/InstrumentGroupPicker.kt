package com.wanderwildwood.onsa.ui.instruments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.wanderwildwood.onsa.ui.PagedColumn
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.instruments.InstrumentGroup
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog

/**
 * The editor's row naming the instrument a tuning is filed under. One line, so the strings
 * below keep their room; a tap opens [InstrumentGroupPicker].
 */
@Composable
fun InstrumentGroupRow(
    group: InstrumentGroup?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(
            stringResource(R.string.instrument_group),
            style = MaterialTheme.typography.titleSmall
        )
        TextMMD(
            stringResource(group?.nameResource ?: R.string.instrument_group_none),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Which instrument a tuning belongs to, or none. Filed under an instrument, a tuning is
 * listed on that instrument's page after its built-in tunings; on its own, it has its own
 * row in the instrument list.
 */
@Composable
fun InstrumentGroupPicker(
    current: InstrumentGroup?,
    onGroupSelected: (group: InstrumentGroup?) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    EInkAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            OutlinedButtonMMD(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.abort)) }
        },
        title = { TextMMD(stringResource(id = R.string.pick_instrument_group)) },
        text = {
            PagedColumn(
                modifier = Modifier.heightIn(max = 420.dp)
            ) {
                item {
                    GroupChoice(
                        label = stringResource(R.string.instrument_group_none),
                        group = null,
                        isCurrent = current == null,
                        onClick = { onGroupSelected(null) }
                    )
                }
                items(InstrumentGroup.entries) { group ->
                    GroupChoice(
                        label = stringResource(group.nameResource),
                        group = group,
                        isCurrent = current == group,
                        onClick = { onGroupSelected(group) }
                    )
                }
            }
        },
    )
}

@Composable
private fun GroupChoice(
    label: String,
    group: InstrumentGroup?,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (group != null) {
            Icon(
                ImageVector.vectorResource(id = group.icon.resourceId),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Spacer(Modifier.size(32.dp))
        }
        Spacer(Modifier.width(16.dp))
        // The current choice in bold, the way the rest of the app marks what is in use.
        TextMMD(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

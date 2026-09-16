package com.wanderwildwood.onsa.ui.stretchtuning

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.onsa.R
import com.wanderwildwood.onsa.ui.misc.rememberNumberFormatter
import com.wanderwildwood.onsa.ui.theme.EInkAlertDialog
import com.wanderwildwood.onsa.ui.theme.TunerTheme
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParsePosition

/** Parse a string, which is allowed to have white spaces.
 * @param string String to be parsed.
 * @return String as float of null if failed.
 */
private fun DecimalFormat.toDoubleOrNull(string: String): Double? {
    return try {
        val trimmed = string.trim()
        val position = ParsePosition(0)
        val result = this.parse(trimmed, position)?.toDouble()
        if (position.index == trimmed.length)
            result
        else
            null
    } catch (_: Exception) {
        null
    }
}
@Composable
fun ModifyStretchTuningLineDialog(
    initialUnstretchedFrequency: Double,
    initialStretchInCents: Double,
    key: Int,
    modifier: Modifier = Modifier,
    onAbortClicked: () -> Unit = {},
    onConfirmedClicked: (
        unstretchedFrequency: Double, stretchInCents: Double, key: Int) -> Unit = {_, _, _ ->}
) {
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocalConfiguration.current.locales[0]
    } else {
        LocalConfiguration.current.locale
    }

    var unstretchedFrequencyAsString by rememberSaveable {
        mutableStateOf("%.2f".format(initialUnstretchedFrequency))
    }
    val numberFormat = remember(locale) {
        NumberFormat.getNumberInstance(locale) as DecimalFormat
    }
    val validUnstretchedFrequencyString by remember { derivedStateOf {
        numberFormat.toDoubleOrNull(unstretchedFrequencyAsString) != null
    }}

    val validUnstretchedFrequencyPositive by remember { derivedStateOf {
        (numberFormat.toDoubleOrNull(unstretchedFrequencyAsString) ?: 1.0) > 0.0
    }}

    var stretchInCentsAsString by rememberSaveable {
        mutableStateOf("%.2f".format(initialStretchInCents))
    }
    val validStretchInCents by remember { derivedStateOf {
        val value = numberFormat.toDoubleOrNull(stretchInCentsAsString)
        value != null
    }}

    EInkAlertDialog(
        onDismissRequest = onAbortClicked,
        confirmButton = {
            OutlinedButtonMMD(
                onClick = {
                    val unstretchedFrequency = numberFormat.toDoubleOrNull(
                        unstretchedFrequencyAsString
                    ) ?: initialUnstretchedFrequency
                    val stretchInCents = numberFormat.toDoubleOrNull(
                        stretchInCentsAsString
                    ) ?: initialStretchInCents
                    onConfirmedClicked(unstretchedFrequency, stretchInCents, key)
                },
                enabled = validUnstretchedFrequencyString && validUnstretchedFrequencyPositive && validStretchInCents,
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.done)) }
        },
        dismissButton = {
            OutlinedButtonMMD(
                onClick = onAbortClicked,
                modifier = Modifier.fillMaxWidth(),
            ) { TextMMD(stringResource(id = R.string.abort)) }
        },
        title = {
            TextMMD(stringResource(R.string.stretched_frequency))
        },
        text = {
            Column {
                TextFieldMMD(
                    value = unstretchedFrequencyAsString,
                    onValueChange = { unstretchedFrequencyAsString = it },
                    label = { TextMMD(stringResource(id = R.string.frequency))},
                    suffix = { TextMMD(stringResource(id = R.string.hertz_str, ""))},
                    isError = !(validUnstretchedFrequencyString && validUnstretchedFrequencyPositive),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    supportingText = if (!validUnstretchedFrequencyString) {
                        { TextMMD(stringResource(R.string.input_is_no_number))}
                    } else if (!validUnstretchedFrequencyPositive) {
                        { TextMMD(stringResource(R.string.value_must_be_larger_zero))}
                    } else {
                        null
                    }
                )
                TextFieldMMD(
                    value = stretchInCentsAsString,
                    onValueChange = { stretchInCentsAsString = it },
                    label = { TextMMD(stringResource(id = R.string.stretch))},
                    suffix = { TextMMD(stringResource(id = R.string.cent_symbol))},
                    isError = !(validStretchInCents),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    supportingText = if (!validStretchInCents) {
                        { TextMMD(stringResource(R.string.input_is_no_number))}
                    } else {
                        null
                    }
                )

            }
        },
    )
}

@Preview(widthDp = 300, heightDp = 500)
@Composable
private fun ModifyStretchTuningLineDialogTest() {
    TunerTheme {
        ModifyStretchTuningLineDialog(
            initialUnstretchedFrequency = 440.65,
            initialStretchInCents = 32.34,
            key = 123
        )
    }
}

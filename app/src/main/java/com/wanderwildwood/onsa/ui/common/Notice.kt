package com.wanderwildwood.onsa.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * One line to say along the foot of whatever screen is up: what was loaded, saved or could not
 * be. It stays until the next press (see the app's root in MainActivity) or a press on it: on
 * this panel a toast that goes by itself could be gone before it was read.
 */
object Notice {
    var text by mutableStateOf<String?>(null)

    fun say(context: Context, text: String) {
        this.text = text
    }

    fun say(context: Context, @StringRes id: Int) {
        text = context.getString(id)
    }
}

@Composable
fun NoticeStrip() {
    val said = Notice.text ?: return
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDividerMMD()
        TextMMD(
            text = said,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (Notice.text == said) Notice.text = null }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

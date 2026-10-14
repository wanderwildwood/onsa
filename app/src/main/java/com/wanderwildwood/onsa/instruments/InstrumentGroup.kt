package com.wanderwildwood.onsa.instruments

import androidx.annotation.StringRes
import com.wanderwildwood.onsa.R

/**
 * The instrument a tuning belongs to.
 *
 * The instrument list shows one row per instrument rather than one per tuning, so a guitar's
 * eight tunings or a banjo's eleven sit behind a single Guitar or Banjo row instead of filling
 * the first screen. Where an instrument has only the one tuning, its row is that tuning and a
 * tap selects it, as before.
 *
 * Names reuse the instruments' own strings where one already says just the instrument, so
 * those stay translated; Guitar, Bass and Banjo are new.
 *
 * The constants are written by name into saved settings and exported files, so they are
 * never renamed.
 */
enum class InstrumentGroup(
    @StringRes val nameResource: Int,
    val icon: InstrumentIcon
) {
    guitar(R.string.instrument_guitar, InstrumentIcon.guitar),
    bass(R.string.instrument_bass, InstrumentIcon.bass),
    ukulele(R.string.ukulele_gcea, InstrumentIcon.ukulele),
    violin(R.string.violin_gdae, InstrumentIcon.violin),
    viola(R.string.viola_cgda, InstrumentIcon.violin),
    cello(R.string.cello_cgda, InstrumentIcon.cello),
    double_bass(R.string.double_bass_eadg, InstrumentIcon.double_bass),
    banjo(R.string.instrument_banjo, InstrumentIcon.banjo)
}

/** The group a saved name stands for, or null for one this version does not know. */
fun String.toInstrumentGroup(): InstrumentGroup? =
    InstrumentGroup.entries.firstOrNull { it.name == this.trim() }

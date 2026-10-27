package com.wanderwildwood.onsa.ui.tuning

import android.os.SystemClock

/** Lets an update through at most once every [minIntervalMs]. The first one always passes. */
class UpdateLimiter(private val minIntervalMs: Long) {
    private var last = Long.MIN_VALUE

    fun allow(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (last != Long.MIN_VALUE && now - last < minIntervalMs)
            return false
        last = now
        return true
    }
}

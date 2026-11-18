package com.wanderwildwood.onsa.metronome

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetronomeFocusTest {
    @Test fun aCallOrAnotherPlayerStopsTheClicks() {
        assertTrue(MetronomeEngine.stopsOn(AudioManager.AUDIOFOCUS_LOSS))
        assertTrue(MetronomeEngine.stopsOn(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT))
    }

    @Test fun aDuckOrAGainDoesNot() {
        assertFalse(MetronomeEngine.stopsOn(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK))
        assertFalse(MetronomeEngine.stopsOn(AudioManager.AUDIOFOCUS_GAIN))
    }

    @Test fun notificationsAreAskedOnceOnAndroid13AndLater() {
        assertTrue(NotificationAsk.needsAsking(sdk = 33, granted = false, askedBefore = false))
        assertFalse(NotificationAsk.needsAsking(sdk = 33, granted = false, askedBefore = true))
        assertFalse(NotificationAsk.needsAsking(sdk = 33, granted = true, askedBefore = false))
        assertFalse(NotificationAsk.needsAsking(sdk = 31, granted = false, askedBefore = false))
    }
}

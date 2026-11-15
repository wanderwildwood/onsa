package com.wanderwildwood.onsa.metronome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.wanderwildwood.onsa.MainActivity
import com.wanderwildwood.onsa.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps the metronome going with the screen off: a foreground service with a notification that
 * says so and stops it. It holds a partial wake lock only while it plays.
 *
 * The service does not start or stop the clicks on the screen's behalf. The view model does
 * that itself, inside the press, and then sends a command; the service follows whatever
 * [MetronomeEngine.playing] says when the command arrives (see [serviceStep]). Only the
 * notification's Stop and the app being swiped away stop the clicks from here.
 */
@AndroidEntryPoint
class MetronomeService : Service() {
    @Inject lateinit var engine: MetronomeEngine
    @Inject lateinit var resources: MetronomeResources

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    /** True once this service has let go by its own hand; see [onDestroy]. */
    private var hasLetGo = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val step = serviceStep(
            play = intent?.action == ACTION_PLAY,
            stopAsked = intent?.action == ACTION_STOP,
            playing = engine.playing.value
        )
        if (intent?.action == ACTION_STOP) engine.stop()
        when (step) {
            ServiceStep.Hold -> hold()
            ServiceStep.HoldThenLetGo -> {
                hold()
                letGo(startId)
            }
            ServiceStep.LetGo -> letGo(startId)
            ServiceStep.Nothing -> {}
        }
        return START_NOT_STICKY
    }

    /** Swiping the app away stops the clicks too; nothing should tick on with no way back to it. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        engine.stop()
        letGo(null)
    }

    override fun onDestroy() {
        // Destroyed by the system rather than by letGo: nothing may tick on without the
        // notification. After letGo the engine is left alone, since by now the reader may have
        // pressed Start again and that run belongs to the next service.
        if (!hasLetGo) engine.stop()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun hold() {
        hasLetGo = false
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else 0
        )
        if (wakeLock == null) {
            wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "onsa:metronome")
                .apply { setReferenceCounted(false); acquire(MAX_WAKE_MILLIS) }
        }
    }

    /**
     * Drops the notification and the wake lock and stops, unless a command newer than
     * [startId] is waiting: stopping regardless would throw that command away, and it may be
     * the Start that follows this Stop. With [startId] null it stops whatever is waiting.
     */
    private fun letGo(startId: Int?) {
        hasLetGo = true
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (startId == null) stopSelf() else stopSelf(startId)
    }

    private fun releaseWakeLock() {
        wakeLock?.release()
        wakeLock = null
    }

    private fun notification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, getString(R.string.metronome),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
            )
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, MetronomeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val s = resources.settings.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metronome)
            .setContentTitle(getString(R.string.metronome_playing))
            .setContentText(getString(R.string.metronome_notification_text, s.bpm, s.beatsPerBar))
            .setContentIntent(open)
            .addAction(0, getString(R.string.metronome_stop), stop)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "metronome"
        private const val NOTIFICATION_ID = 7
        private const val ACTION_PLAY = "com.wanderwildwood.onsa.metronome.PLAY"
        private const val ACTION_QUIET = "com.wanderwildwood.onsa.metronome.QUIET"
        private const val ACTION_STOP = "com.wanderwildwood.onsa.metronome.STOP"
        /** A safety net: a lock that is somehow never let go still lets go after four hours. */
        private const val MAX_WAKE_MILLIS = 4L * 60 * 60 * 1000

        /**
         * Tells the service that the metronome was started, or that its settings changed while
         * it plays (the notification shows the tempo). Call it after [MetronomeEngine.start].
         */
        fun play(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, MetronomeService::class.java).setAction(ACTION_PLAY)
            )
        }

        /** Tells the service that the metronome was stopped. Call it after [MetronomeEngine.stop]. */
        fun quiet(context: Context) {
            try {
                context.startService(Intent(context, MetronomeService::class.java).setAction(ACTION_QUIET))
            } catch (_: IllegalStateException) {
                // The app is in the background, where a plain start is refused. A service that
                // is up is taken down directly; one that is not has nothing to take down.
                context.stopService(Intent(context, MetronomeService::class.java))
            }
        }
    }
}

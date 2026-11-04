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
 */
@AndroidEntryPoint
class MetronomeService : Service() {
    @Inject lateinit var engine: MetronomeEngine
    @Inject lateinit var resources: MetronomeResources

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlaying()
            return START_NOT_STICKY
        }
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
        engine.start(resources.settings.value)
        return START_NOT_STICKY
    }

    /** Swiping the app away stops the clicks too; nothing should tick on with no way back to it. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopPlaying()
    }

    override fun onDestroy() {
        engine.stop()
        wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun stopPlaying() {
        engine.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        private const val ACTION_STOP = "com.wanderwildwood.onsa.metronome.STOP"
        /** A safety net: a lock that is somehow never let go still lets go after four hours. */
        private const val MAX_WAKE_MILLIS = 4L * 60 * 60 * 1000

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MetronomeService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, MetronomeService::class.java).setAction(ACTION_STOP))
        }
    }
}

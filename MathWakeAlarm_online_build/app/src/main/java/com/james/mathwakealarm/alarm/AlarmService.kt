package com.james.mathwakealarm.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.james.mathwakealarm.R

class AlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var startVolume = 0.1f
    private var currentVolume = 0.1f

    private val volumeRamp = object : Runnable {
        override fun run() {
            currentVolume = (currentVolume + VOLUME_STEP).coerceAtMost(1f)
            mediaPlayer?.setVolume(currentVolume, currentVolume)
            if (currentVolume < 1f) {
                handler.postDelayed(this, VOLUME_RAMP_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAlarm()
            ACTION_START -> {
                val required = intent.getIntExtra(EXTRA_REQUIRED_CORRECT, 5).coerceIn(1, 20)
                startVolume = intent.getIntExtra(EXTRA_START_VOLUME_PERCENT, 10)
                    .coerceIn(1, 50) / 100f
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                AlarmSessionRepository(this).startNew(required)
                startAlarm(vibrate)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseResources()
        super.onDestroy()
    }

    private fun startAlarm(shouldVibrate: Boolean) {
        val notification = buildNotification()
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)

        acquireWakeLock()
        requestAudioFocus()
        startAudio()
        if (shouldVibrate) startVibration()
    }

    private fun startAudio() {
        mediaPlayer?.release()
        currentVolume = startVolume
        val resource = resources.openRawResourceFd(R.raw.alarm_loop)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(resource.fileDescriptor, resource.startOffset, resource.length)
            resource.close()
            isLooping = true
            setVolume(currentVolume, currentVolume)
            prepare()
            start()
        }
        handler.removeCallbacks(volumeRamp)
        handler.postDelayed(volumeRamp, VOLUME_RAMP_INTERVAL_MS)
    }

    private fun requestAudioFocus() {
        audioManager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { }
                .build()
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 600, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:alarm-wake-lock",
        ).apply { acquire(30 * 60 * 1000L) }
    }

    private fun buildNotification(): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            9001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Solve the maths questions to stop the alarm")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopAlarm() {
        AlarmSessionRepository(this).complete()
        releaseResources()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseResources() {
        handler.removeCallbacks(volumeRamp)
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
        audioFocusRequest = null
        audioManager = null

        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        const val ACTION_START = "com.james.mathwakealarm.action.START_ALARM"
        const val ACTION_STOP = "com.james.mathwakealarm.action.STOP_ALARM"
        const val EXTRA_REQUIRED_CORRECT = "required_correct"
        const val EXTRA_START_VOLUME_PERCENT = "start_volume_percent"
        const val EXTRA_VIBRATE = "vibrate"
        private const val CHANNEL_ID = "wake_alarm_channel"
        private const val NOTIFICATION_ID = 7001
        private const val VOLUME_RAMP_INTERVAL_MS = 10_000L
        private const val VOLUME_STEP = 0.1f
    }
}

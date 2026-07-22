package com.james.mathwakealarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.james.mathwakealarm.data.AlarmPreferences

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
        val settings = AlarmPreferences(context).load()

        val serviceIntent = Intent(context, AlarmService::class.java)
            .setAction(AlarmService.ACTION_START)
            .putExtra(AlarmService.EXTRA_REQUIRED_CORRECT, settings.requiredCorrectAnswers)
            .putExtra(AlarmService.EXTRA_START_VOLUME_PERCENT, settings.startingVolumePercent)
            .putExtra(AlarmService.EXTRA_VIBRATE, settings.vibrate)

        ContextCompat.startForegroundService(context, serviceIntent)

        if (!isTest && settings.enabled) {
            AlarmScheduler(context).schedule(settings)
        }
    }

    companion object {
        const val EXTRA_IS_TEST = "is_test_alarm"
    }
}

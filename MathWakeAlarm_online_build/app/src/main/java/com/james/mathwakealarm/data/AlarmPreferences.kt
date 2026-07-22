package com.james.mathwakealarm.data

import android.content.Context
import java.time.DayOfWeek

class AlarmPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AlarmSettings {
        val storedDays = preferences.getStringSet(KEY_DAYS, null)
        val days = storedDays
            ?.mapNotNull { value -> runCatching { DayOfWeek.valueOf(value) }.getOrNull() }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: AlarmSettings().activeDays

        return AlarmSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            hour = preferences.getInt(KEY_HOUR, 6).coerceIn(0, 23),
            minute = preferences.getInt(KEY_MINUTE, 30).coerceIn(0, 59),
            activeDays = days,
            requiredCorrectAnswers = preferences.getInt(KEY_REQUIRED, 5).coerceIn(1, 20),
            startingVolumePercent = preferences.getInt(KEY_START_VOLUME, 10).coerceIn(1, 50),
            vibrate = preferences.getBoolean(KEY_VIBRATE, true),
        )
    }

    fun save(settings: AlarmSettings) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .putStringSet(KEY_DAYS, settings.activeDays.map { it.name }.toSet())
            .putInt(KEY_REQUIRED, settings.requiredCorrectAnswers)
            .putInt(KEY_START_VOLUME, settings.startingVolumePercent)
            .putBoolean(KEY_VIBRATE, settings.vibrate)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "alarm_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_DAYS = "days"
        private const val KEY_REQUIRED = "required"
        private const val KEY_START_VOLUME = "start_volume"
        private const val KEY_VIBRATE = "vibrate"
    }
}

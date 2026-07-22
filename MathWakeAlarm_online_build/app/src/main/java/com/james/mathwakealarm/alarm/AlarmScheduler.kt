package com.james.mathwakealarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.james.mathwakealarm.MainActivity
import com.james.mathwakealarm.data.AlarmPreferences
import com.james.mathwakealarm.data.AlarmSettings
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(settings: AlarmSettings): Result<ZonedDateTime> = runCatching {
        require(settings.activeDays.isNotEmpty()) { "Choose at least one day" }
        check(canScheduleExactAlarms()) { "Exact alarm permission has not been granted" }

        val next = NextAlarmCalculator.nextTrigger(settings)
        val triggerIntent = alarmPendingIntent(REGULAR_ALARM_REQUEST_CODE, isTest = false)
        val showIntent = PendingIntent.getActivity(
            context,
            SHOW_APP_REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(next.toInstant().toEpochMilli(), showIntent),
            triggerIntent,
        )
        next
    }

    fun scheduleFromStoredSettings(): Result<ZonedDateTime>? {
        val settings = AlarmPreferences(context).load()
        return if (settings.enabled) schedule(settings) else null
    }

    fun scheduleTest(secondsFromNow: Int = 10): Result<ZonedDateTime> = runCatching {
        check(canScheduleExactAlarms()) { "Exact alarm permission has not been granted" }
        val trigger = ZonedDateTime.now().plusSeconds(secondsFromNow.toLong())
        val triggerIntent = alarmPendingIntent(TEST_ALARM_REQUEST_CODE, isTest = true)
        val showIntent = PendingIntent.getActivity(
            context,
            SHOW_APP_REQUEST_CODE + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(trigger.toInstant().toEpochMilli(), showIntent),
            triggerIntent,
        )
        trigger
    }

    fun cancelRegular() {
        alarmManager.cancel(alarmPendingIntent(REGULAR_ALARM_REQUEST_CODE, isTest = false))
    }

    fun cancelTest() {
        alarmManager.cancel(alarmPendingIntent(TEST_ALARM_REQUEST_CODE, isTest = true))
    }

    fun exactAlarmSettingsIntent(): Intent = Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        android.net.Uri.parse("package:${context.packageName}"),
    )

    private fun alarmPendingIntent(requestCode: Int, isTest: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_IS_TEST, isTest)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val REGULAR_ALARM_REQUEST_CODE = 8001
        private const val TEST_ALARM_REQUEST_CODE = 8002
        private const val SHOW_APP_REQUEST_CODE = 8101
    }
}

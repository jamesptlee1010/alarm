package com.james.mathwakealarm.alarm

import com.james.mathwakealarm.data.AlarmSettings
import java.time.ZonedDateTime

object NextAlarmCalculator {
    fun nextTrigger(settings: AlarmSettings, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        require(settings.activeDays.isNotEmpty()) { "At least one active day is required" }

        for (daysAhead in 0..7) {
            val candidate = now
                .plusDays(daysAhead.toLong())
                .withHour(settings.hour)
                .withMinute(settings.minute)
                .withSecond(0)
                .withNano(0)

            if (candidate.dayOfWeek in settings.activeDays && candidate.isAfter(now)) {
                return candidate
            }
        }

        error("Unable to calculate the next alarm")
    }
}

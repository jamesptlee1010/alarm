package com.james.mathwakealarm.alarm

import com.james.mathwakealarm.data.AlarmSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmCalculatorTest {
    private val zone = ZoneId.of("Australia/Brisbane")

    @Test
    fun usesSameDayWhenTimeIsStillAhead() {
        val now = ZonedDateTime.of(2026, 7, 22, 6, 0, 0, 0, zone)
        val settings = AlarmSettings(
            hour = 6,
            minute = 30,
            activeDays = setOf(DayOfWeek.WEDNESDAY),
        )
        assertEquals(
            ZonedDateTime.of(2026, 7, 22, 6, 30, 0, 0, zone),
            NextAlarmCalculator.nextTrigger(settings, now),
        )
    }

    @Test
    fun movesToNextSelectedDayWhenTimePassed() {
        val now = ZonedDateTime.of(2026, 7, 22, 7, 0, 0, 0, zone)
        val settings = AlarmSettings(
            hour = 6,
            minute = 30,
            activeDays = setOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
        )
        assertEquals(
            ZonedDateTime.of(2026, 7, 23, 6, 30, 0, 0, zone),
            NextAlarmCalculator.nextTrigger(settings, now),
        )
    }
}

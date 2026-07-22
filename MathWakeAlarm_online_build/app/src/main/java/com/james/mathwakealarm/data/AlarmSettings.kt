package com.james.mathwakealarm.data

import java.time.DayOfWeek

data class AlarmSettings(
    val enabled: Boolean = false,
    val hour: Int = 6,
    val minute: Int = 30,
    val activeDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    ),
    val requiredCorrectAnswers: Int = 5,
    val startingVolumePercent: Int = 10,
    val vibrate: Boolean = true,
)

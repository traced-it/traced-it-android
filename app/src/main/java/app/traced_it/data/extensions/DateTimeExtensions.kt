package app.traced_it.data.extensions

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus

fun LocalDateTime.plusOffset(dateOffset: Int, hourOffset: Int, minuteOffset: Int): LocalDateTime =
    date.plusDateOffset(dateOffset)
        .atTime(hour.plusHourOffset(hourOffset), minute.plusMinuteOffset(minuteOffset), second, nanosecond)

fun LocalDate.plusDateOffset(offset: Int): LocalDate =
    plus(DatePeriod(days = offset))

fun Int.plusHourOffset(offset: Int): Int =
    plus(offset).mod(24)

fun Int.plusMinuteOffset(offset: Int): Int =
    plus(offset).mod(60)

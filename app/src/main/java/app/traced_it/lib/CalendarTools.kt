package app.traced_it.lib

import androidx.compose.runtime.Immutable
import java.util.*

@Immutable
data class Day(val year: Int, val month: Int, val date: Int)

fun gregorianCalendar(
    zone: TimeZone,
    time: Date? = null,
    day: Day? = null,
    hour: Int? = null,
    minute: Int? = null,
): Calendar =
    GregorianCalendar.getInstance(zone).apply {
        if (time != null) this.time = time
        if (day != null) this.day = day
        if (hour != null) this.hour = hour
        if (minute != null) this.minute = minute
    }

fun gregorianCalendar(
    zone: TimeZone,
    timeInMillis: Long? = null,
    day: Day? = null,
    hour: Int? = null,
    minute: Int? = null,
): Calendar = gregorianCalendar(zone, time = timeInMillis?.let { Date(it) }, day = day, hour = hour, minute = minute)

fun gregorianCalendar(
    zone: TimeZone,
    day: Day? = null,
    hour: Int? = null,
    minute: Int? = null,
): Calendar = gregorianCalendar(zone, time = null, day = day, hour = hour, minute = minute)

var Calendar.day
    get() = Day(this.get(Calendar.YEAR), this.get(Calendar.MONTH), this.get(Calendar.DATE))
    set(day) {
        this.set(day.year, day.month, day.date)
    }

var Calendar.hour
    get() = this.get(Calendar.HOUR_OF_DAY)
    set(hour) {
        this.set(Calendar.HOUR_OF_DAY, hour)
    }

var Calendar.minute
    get() = this.get(Calendar.MINUTE)
    set(minute) {
        this.set(Calendar.MINUTE, minute)
    }

fun Calendar.addDays(days: Int) {
    this.add(Calendar.DATE, days)
}

fun Calendar.copy(day: Day? = null, hour: Int? = null, minute: Int? = null): Calendar =
    gregorianCalendar(this.timeZone, timeInMillis = this.timeInMillis, day = day, hour = hour, minute = minute)

fun Calendar.generateDaysList(startOffset: Int, size: Int): List<Day> {
    val calendar = this.copy(hour = 12)
    calendar.addDays(startOffset)
    return List(size) { i ->
        if (i != 0) {
            calendar.addDays(1)
        }
        calendar.day
    }
}

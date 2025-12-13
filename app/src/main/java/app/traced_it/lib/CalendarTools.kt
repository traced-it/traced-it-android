package app.traced_it.lib

import java.util.*

data class Day(val year: Int, val month: Int, val date: Int)

fun gregorianCalendar(
    zone: TimeZone,
    time: Long? = null,
    day: Day? = null,
    hour: Int? = null,
    minute: Int? = null,
): Calendar =
    GregorianCalendar.getInstance(zone).apply {
        if (time != null) this.time = Date(time)
        if (day != null) this.day = day
        if (hour != null) this.hour = hour
        if (minute != null) this.minute = minute
    }

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

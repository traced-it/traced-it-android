package app.traced_it.lib

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class CalendarToolsTest {
    @Test
    fun generateDaysList() {
        val calendar = gregorianCalendar(TimeZone.getDefault(), day = Day(2025, 11, 31), hour = 16, minute = 31)
        assertEquals(
            listOf(
                Day(2025, 11, 29),
                Day(2025, 11, 30),
                Day(2025, 11, 31),
                Day(2026, 0, 1),
                Day(2026, 0, 2),
            ),
            calendar.generateDaysList(-2, 5),
        )
    }
}

package app.traced_it.lib

import org.junit.Assert.assertEquals
import org.junit.Test

class IntRangeExtensionsTest {
    @Test
    fun generateNumbersList() {
        assertEquals(
            listOf(4, 5, 0, 1, 2, 3, 4, 5, 0),
            (0..5).generateNumbersList(-2, 9),
        )
    }
}

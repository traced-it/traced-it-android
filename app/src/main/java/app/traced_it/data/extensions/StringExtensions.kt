package app.traced_it.data.extensions

import java.text.NumberFormat
import java.text.ParseException

fun NumberFormat.toDoubleOrNull(value: String): Double? =
    try {
        parse(value)?.toDouble()
    } catch (_: ParseException) {
        null
    }

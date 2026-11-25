package app.traced_it.data.extensions

fun Int.toTwoDigitString(): String =
    if (this in 0..9) {
        "0$this"
    } else {
        toString()
    }

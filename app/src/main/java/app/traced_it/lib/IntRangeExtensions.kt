package app.traced_it.lib

fun IntRange.generateNumbersList(startOffset: Int, size: Int): List<Int> {
    val rangeSize = (this.last - this.first + 1)
    return List(size) { i ->
        (this.first + startOffset + i).mod(rangeSize)
    }
}

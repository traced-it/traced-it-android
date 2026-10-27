package app.traced_it.data.database

val queryRegex: Regex = """(AND|OR|NOT)|(")([^"]*)(")|([-^]?)(\S+)""".toRegex()
val phraseCharacterRegex: Regex = """[\s-]""".toRegex()

private fun String.quoteTerm(): String {
    val quote = if (contains(phraseCharacterRegex)) "\"" else ""
    return quote + replace("\"", "\"\"").replace(":", "\\:") + quote
}

fun String.toFullTextQueryExpression(): String =
    queryRegex.findAll(this).joinToString(" ") {
        when {
            // Operator
            it.groupValues[1].isNotEmpty() -> it.value

            // Phrase
            it.groupValues[2].isNotEmpty() -> it.groupValues[3].quoteTerm()

            // Term with first (^) or unary (-) operator
            it.groupValues[5].isNotEmpty() -> it.groupValues[5] + (it.groupValues[6] + "*").quoteTerm()

            // Term
            else -> ("*" + it.groupValues[0] + "*").quoteTerm()
        }
    }

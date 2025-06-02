package app.traced_it.data.local.database

val queryRegex: Regex = """(AND|OR|NOT)|(")([^"]*)(")|([-^]?)(\S+)""".toRegex()
val phraseCharacterRegex: Regex = """[\s-]""".toRegex()

private fun quoteTerm(unsafeTerm: String): String {
    val quote = if (unsafeTerm.contains(phraseCharacterRegex)) "\"" else ""
    return quote + unsafeTerm.replace("\"", "\"\"").replace(":", "\\:") + quote
}

fun createFullTextQueryExpression(unsafeQuery: String): String =
    queryRegex.findAll(unsafeQuery).map {
        when {
            // Operator
            it.groupValues[1].isNotEmpty() -> it.value

            // Phrase
            it.groupValues[2].isNotEmpty() -> quoteTerm(it.groupValues[3])

            // Term with first (^) or unary (-) operator
            it.groupValues[5].isNotEmpty() -> it.groupValues[5] + quoteTerm(it.groupValues[6] + "*")

            // Term
            else -> quoteTerm("*" + it.groupValues[0] + "*")
        }
    }.joinToString(" ")

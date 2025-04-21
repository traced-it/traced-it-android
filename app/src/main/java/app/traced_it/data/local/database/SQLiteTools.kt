package app.traced_it.data.local.database

val queryRegex: Regex = """(AND|OR|NOT)|([-^]?")([^"]*)(")|([-^]?)(\S+)""".toRegex()

private fun quoteTerm(unsafeTerm: String): String = unsafeTerm
    .replace("\"", "\"\"")
    .replace(":", "\\:")

fun sanitizeSQLiteMatchQuery(unsafeQuery: String): String =
    queryRegex.findAll(unsafeQuery).map {
        when {
            // Operator
            it.groupValues[1].isNotEmpty() -> it.value

            // Phrase
            it.groupValues[2].isNotEmpty() -> it.groupValues[2] + quoteTerm(it.groupValues[3]) + it.groupValues[4]

            // Term with first or unary operator
            it.groupValues[5].isNotEmpty() -> it.groupValues[5] + quoteTerm(it.groupValues[6]) + "*"

            // Term
            else -> "*" + quoteTerm(it.groupValues[0]) + "*"
        }
    }
        .joinToString(" ")

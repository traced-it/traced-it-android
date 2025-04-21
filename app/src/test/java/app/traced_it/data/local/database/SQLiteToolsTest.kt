package app.traced_it.data.local.database

import junit.framework.TestCase.assertEquals
import org.junit.Test

class SQLiteToolsTest {
    @Test
    fun testSingleWord() {
        assertEquals(
            "*one-word*",
            sanitizeSQLiteMatchQuery("one-word"),
        )
    }

    @Test
    fun testTwoWords() {
        assertEquals(
            "*two* *words*",
            sanitizeSQLiteMatchQuery("two words"),
        )
    }

    @Test
    fun testColon() {
        assertEquals(
            "*word\\:with-colon* *standalone* *\\:*",
            sanitizeSQLiteMatchQuery("word:with-colon standalone :"),
        )
    }

    @Test
    fun testOperators() {
        assertEquals(
            "*two* AND *words* OR *with* NOT *operators*",
            sanitizeSQLiteMatchQuery("two AND words OR with NOT operators"),
        )
    }

    @Test
    fun testFirstOperatorQuote() {
        assertEquals(
            "*first* ^operator*",
            sanitizeSQLiteMatchQuery("first ^operator"),
        )
    }

    @Test
    fun testUnaryOperator() {
        assertEquals(
            "*unary* -operator*",
            sanitizeSQLiteMatchQuery("unary -operator"),
        )
    }

    @Test
    fun testWordAndPhrase() {
        assertEquals(
            "*word* \"and phrase\"",
            sanitizeSQLiteMatchQuery("word \"and phrase\""),
        )
    }

    @Test
    fun testPhraseWithUnaryOperator() {
        assertEquals(
            "-\"phrase with unary operator\"",
            sanitizeSQLiteMatchQuery("-\"phrase with unary operator\""),
        )
    }

    @Test
    fun testQuoteInsideWord() {
        assertEquals(
            "*quote\"\"inside-word*",
            sanitizeSQLiteMatchQuery("quote\"inside-word"),
        )
    }

    @Test
    fun testQuoteInsidePhrase() {
        assertEquals(
            "\"quote\" *inside* *phrase\"\"*",
            sanitizeSQLiteMatchQuery("\"quote\"inside phrase\""),
        )
    }

    @Test
    fun testUnmatchedQuote() {
        assertEquals(
            "*\"\"unmatched* *quote*",
            sanitizeSQLiteMatchQuery("\"unmatched quote"),
        )
    }

}

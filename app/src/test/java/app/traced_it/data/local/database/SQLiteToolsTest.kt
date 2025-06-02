package app.traced_it.data.local.database

import junit.framework.TestCase.assertEquals
import org.junit.Test

class SQLiteToolsTest {
    @Test
    fun testSingleWord() {
        assertEquals(
            "*one_word*",
            createFullTextQueryExpression("one_word"),
        )
    }

    @Test
    fun testTwoWords() {
        assertEquals(
            "*two* *words*",
            createFullTextQueryExpression("two words"),
        )
    }

    @Test
    fun testWordWithDash() {
        assertEquals(
            "\"*word-with-dash*\"",
            createFullTextQueryExpression("word-with-dash"),
        )
    }

    @Test
    fun testColon() {
        assertEquals(
            "*word\\:with_colon* *standalone* *\\:*",
            createFullTextQueryExpression("word:with_colon standalone :"),
        )
    }

    @Test
    fun testOperators() {
        assertEquals(
            "*two* AND *words* OR *with* NOT *operators*",
            createFullTextQueryExpression("two AND words OR with NOT operators"),
        )
    }

    @Test
    fun testFirstOperatorQuote() {
        assertEquals(
            "*first* ^operator*",
            createFullTextQueryExpression("first ^operator"),
        )
    }

    @Test
    fun testUnaryOperator() {
        assertEquals(
            "*unary* -operator*",
            createFullTextQueryExpression("unary -operator"),
        )
    }

    @Test
    fun testWordAndPhrase() {
        assertEquals(
            "*word* \"and phrase\"",
            createFullTextQueryExpression("word \"and phrase\""),
        )
    }

    @Test
    fun testPhraseWithDash() {
        assertEquals(
            "\"phrase with-dash\"",
            createFullTextQueryExpression("\"phrase with-dash\""),
        )
    }

    @Test
    fun testPhraseWithoutSpaceOrDash() {
        assertEquals(
            "phrase_without_space_or_dash",
            createFullTextQueryExpression("\"phrase_without_space_or_dash\""),
        )
    }

    @Test
    fun testPhraseWithUnaryOperator() {
        assertEquals(
            "-\"\"phrase* *with* *unary* *operator\"\"*",
            createFullTextQueryExpression("-\"phrase with unary operator\""),
        )
    }

    @Test
    fun testQuoteInsideWord() {
        assertEquals(
            "*quote\"\"inside_word*",
            createFullTextQueryExpression("quote\"inside_word"),
        )
    }

    @Test
    fun testQuoteInsidePhrase() {
        assertEquals(
            "quote *inside* *phrase\"\"*",
            createFullTextQueryExpression("\"quote\"inside phrase\""),
        )
    }

    @Test
    fun testUnmatchedQuote() {
        assertEquals(
            "*\"\"unmatched* *quote*",
            createFullTextQueryExpression("\"unmatched quote"),
        )
    }

}

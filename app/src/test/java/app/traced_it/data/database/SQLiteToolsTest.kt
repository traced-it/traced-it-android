package app.traced_it.data.database

import junit.framework.TestCase.assertEquals
import org.junit.Test

class SQLiteToolsTest {
    @Test
    fun toFullTextQueryExpression_testSingleWord() {
        assertEquals(
            "*one_word*",
            "one_word".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testTwoWords() {
        assertEquals(
            "*two* *words*",
            "two words".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testWordWithDash() {
        assertEquals(
            "\"*word-with-dash*\"",
            "word-with-dash".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testColon() {
        assertEquals(
            "*word\\:with_colon* *standalone* *\\:*",
            "word:with_colon standalone :".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testOperators() {
        assertEquals(
            "*two* AND *words* OR *with* NOT *operators*",
            "two AND words OR with NOT operators".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testFirstOperatorQuote() {
        assertEquals(
            "*first* ^operator*",
            "first ^operator".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testUnaryOperator() {
        assertEquals(
            "*unary* -operator*",
            "unary -operator".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testWordAndPhrase() {
        assertEquals(
            "*word* \"and phrase\"",
            "word \"and phrase\"".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testPhraseWithDash() {
        assertEquals(
            "\"phrase with-dash\"",
            "\"phrase with-dash\"".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testPhraseWithoutSpaceOrDash() {
        assertEquals(
            "phrase_without_space_or_dash",
            "\"phrase_without_space_or_dash\"".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testPhraseWithUnaryOperator() {
        assertEquals(
            "-\"\"phrase* *with* *unary* *operator\"\"*",
            "-\"phrase with unary operator\"".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testQuoteInsideWord() {
        assertEquals(
            "*quote\"\"inside_word*",
            "quote\"inside_word".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testQuoteInsidePhrase() {
        assertEquals(
            "quote *inside* *phrase\"\"*",
            "\"quote\"inside phrase\"".toFullTextQueryExpression(),
        )
    }

    @Test
    fun toFullTextQueryExpression_testUnmatchedQuote() {
        assertEquals(
            "*\"\"unmatched* *quote*",
            "\"unmatched quote".toFullTextQueryExpression(),
        )
    }

}

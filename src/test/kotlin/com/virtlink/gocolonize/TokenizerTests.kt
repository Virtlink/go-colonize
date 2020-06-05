package com.virtlink.gocolonize

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TokenizerTests {

    @Test
    fun test1() {
        val input = "aa bb++ c=d /* e fg != hi */ jk \"l m \\\\\\\\\\\"\" `raw string` fin"

        val tokens = InputTokenizer.tokenize(input)

        assertEquals(listOf(listOf("aa", " ", "bb", "++", " ", "c", "=", "d", " ", "/* e fg != hi */", " ", "jk", " ", "\"l m \\\\\\\\\\\"\"", " ", "`raw string`", " ", "fin")), tokens)
    }

    @Test
    fun test2() {
        val input = "aa bb++\nc=d /* e\n fg != hi */ jk \"l m \\\\\\\\\\\"\" `raw\nstring` fin"

        val tokens = InputTokenizer.tokenize(input)

        assertEquals(listOf(listOf("aa", " ", "bb", "++", "\n"), listOf("c", "=", "d", " ", "/* e\n fg != hi */", " ", "jk", " ", "\"l m \\\\\\\\\\\"\"", " ", "`raw\nstring`", " ", "fin")), tokens)
    }

}
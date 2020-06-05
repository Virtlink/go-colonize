package com.virtlink.gocolonize

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TokenizerTests {

    @Test
    fun test1() {
        val input = "aa bb++ c=d /* e fg != hi */ jk \"l m \\\\\\\\\\\"\" `raw string` fin"
        val sut = InputTokenizer()

        val tokens = sut.tokenizeLine(input)

        assertEquals(listOf("aa", "bb", "++", "c", "=", "d", "/* e fg != hi */", "jk", "\"l m \\\\\\\\\\\"\"", "`raw string`", "fin"), tokens)
    }

//    @Test
//    fun test2() {
//        val input = "aa bb++ c=d /* e\n fg != hi */ jk \"l m \\\\\\\\\\\"\" `raw\nstring` fin"
//        val sut = InputTokenizer()
//
//        val tokens = sut.tokenizeLine(input)
//
//        assertEquals(listOf("aa", "bb", "++", "c", "=", "d", "/* e", " fg != hi */", "jk", "\"l m \\\\\\\\\\\"\"", "`raw", "string`", "fin"), tokens)
//    }

}
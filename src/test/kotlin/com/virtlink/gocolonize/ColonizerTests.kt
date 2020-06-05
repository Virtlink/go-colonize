package com.virtlink.gocolonize

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.InputStreamReader

class ColonizerTests {

//    private val colonizeLineTestData = listOf(
//        "aa" to "aa;",
//        "aa aa" to "aa aa;",
//        "" to ""
//    )
//
//    @TestFactory
//    fun testColonizeLine() = colonizeLineTestData.map { (input, expected) ->
//        DynamicTest.dynamicTest("\"$input\" -> \"$expected\"") {
//            val actual = Colonizer.colonizeLine(input)
//            assertEquals(expected, actual)
//        }
//    }

    private val colonizeFileTestData = listOf(
        "64bit.go" to "64bit.expected.go"
    )

    @TestFactory
    @Disabled
    fun testColonizeFile() = colonizeFileTestData.map { (inputFile, expectedFile) ->
        DynamicTest.dynamicTest("$inputFile -> $expectedFile") {
            val expectedString = javaClass.getResourceAsStream(expectedFile).use { inputStream ->
                InputStreamReader(inputStream).use { it.readText() }
            }

            val actualString = javaClass.getResourceAsStream(inputFile).use { InputStreamReader(it).use { inputReader ->
                val output = StringBuilder()
                Colonizer.colonize(inputReader, output)
                output.toString()
            } }

            assertEquals(expectedString, actualString)
        }
    }

}
package com.virtlink.gocolonize

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.InputStreamReader
import java.nio.file.Paths

class ColonizerTests {

    private val colonizeLineTestData = listOf(
        "a",
        "_x9",
        "ThisVariableIsExported",

        "αβ",
        "42",
        //"4_2",
        "0600",
        //"0_600",
        //"0o600",
        //"0O600",
        "0xBadFace",
        //"0xBad_Face",
        //"0x_67_7a_2f_cc_40_c6",
        "170141183460469231731687303715884105727",
        //"170_141183_460469_231731_687303_715884_105727",

        "0.",
        "72.40",
        "072.40",
        "2.71828",
        "1.e+0",
        "6.67428e-11",
        "1E6",
        ".25",
        ".12345E+5",
        //"1_5.",
        //"0.15e+0_2",

        //"0x1p-2",
        "0x2.p10",
        "0x1.Fp+0",
        //"0X.8p-0",
        //"0X_1FFFP-16",
        "0x15e-2",

        "0i",
        "0123i",
        //"0o123i",
        //"0xabci",
        "0.i",
        "2.71828i",
        "1.e+0i",
        "6.67428e-11i",
        "1E6i",
        ".25i",
        ".12345E+5i",
        //"0x1p-2i",

        "'a'",
        "'ä'",
        "'本'",
        "'\\t'",
        "'\\000'",
        "'\\007'",
        "'\\377'",
        "'\\x07'",
        "'\\xff'",
        "'\\u12e4'",
        "'\\U00101234'",
        "'\\''",

        "`abc`",
        "`\\n\n\\n`",
        "\"\\n\"",
        "\"\\\"\"",
        "\"Hello, world!\\n\"",
        "\"日本語\"",
        "\"\\u65e5本\\U00008a9e\"",
        "\"\\xff\\u00FF\"",

        "\"日本語\"",
        "`日本語`",
        "\"\\u65e5\\u672c\\u8a9e\"",
        "\"\\U000065e5\\U0000672c\\U00008a9e\"",
        "\"\\xe6\\x97\\xa5\\xe6\\x9c\\xac\\xe8\\xaa\\x9e\""
    )

    @TestFactory
    fun testColonizeSingleToken() = colonizeLineTestData.map { input ->
        DynamicTest.dynamicTest("colonize \"$input\" -> \"$input;\"") {
            val outputWriter = StringBuilder()
            Colonizer(OnParseError.Fatal).colonize("package x; var _ = $input", outputWriter, null)
            val actual = outputWriter.toString()
            assertEquals("package x; var _ = $input;", actual)
        }
    }

    private val colonizeFileTestData = listOf(
        //"slice3err.go" to "slice3err.expected.go",
        "issue4776.go" to "issue4776.expected.go",
        "example.go" to "example.expected.go",
        "fibo.go" to "fibo.expected.go",
        "index.go" to "index.expected.go",
        "alias2.go" to "alias2.expected.go",
    )

    @TestFactory
    fun testColonizeFile() = colonizeFileTestData.map { (inputFile, expectedFile) ->
        DynamicTest.dynamicTest("$inputFile -> $expectedFile") {
            val expectedString = javaClass.getResourceAsStream(expectedFile).use { inputStream ->
                InputStreamReader(inputStream).use { it.readText() }
            }

            val actualString = javaClass.getResourceAsStream(inputFile).use { InputStreamReader(it).use { inputReader ->
                val str = inputReader.readText()
                val output = StringBuilder()
                Colonizer(OnParseError.Warn).colonize(str, output, Paths.get(inputFile))
                output.toString()
            } }

            assertEquals(expectedString, actualString)
        }
    }

}
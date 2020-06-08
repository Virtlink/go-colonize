package com.virtlink.gocolonize

import java.io.*
import java.lang.Appendable

object Colonizer {

    private const val newline = "\\n"
    private const val unicode_char = "[^\\n]"
    private const val unicode_letter = "\\p{L}"
    private const val unicode_digit = "\\p{N}"
    private const val letter = "[${unicode_letter}_]"

    private const val identifier = "$letter[$unicode_letter${unicode_digit}_]*"

    private const val decimal_digit = "[0-9]"
    private const val binary_digit = "[0-1]"
    private const val octal_digit = "[0-7]"
    private const val hex_digit = "[0-9a-fA-F]"

    private const val decimal_digits = "(?:$decimal_digit(?:_?$decimal_digit)*)"
    private const val binary_digits = "(?:$binary_digit(?:_?$binary_digit)*)"
    private const val octal_digits = "(?:$octal_digit(?:_?$octal_digit)*)"
    private const val hex_digits = "(?:$hex_digit(?:_?$hex_digit)*)"

    private const val decimal_lit = "(?:0|[1-9](?:_?$decimal_digits)?)"
    private const val binary_lit = "(?:0[bB]_?$binary_digits)"
    private const val octal_lit = "(?:0[oO]?_?$octal_digits)"
    private const val hex_lit = "(?:0[xX]_?$hex_digits)"

    private const val decimal_exponent = "(?:[eE][+\\-]?$decimal_digits)"
    private const val decimal_float_lit =
        "(?:$decimal_digits$decimal_exponent)|(?:\\.$decimal_digits$decimal_exponent?)"

    private const val hex_exponent = "(?:[pP][+\\-]?$decimal_digits)"
    private const val hex_mantissa = "(?:(?:_?$hex_digits\\.$hex_digits?)|(?:_?$hex_digits)|(?:\\.$hex_digits))"
    private const val hex_float_lit = "(?:0[xX]$hex_mantissa$hex_exponent)"

    private const val int_lit = "$decimal_lit|$binary_lit|$octal_lit|$hex_lit"
    private const val float_lit = "(?:$decimal_float_lit|$hex_float_lit)"

    private const val imaginary_lit = "(?:(?:$decimal_digits|$int_lit|$float_lit)i)"

    private const val escaped_char = "(?:\\\\[abfnrtv\\\\'\"])"
    private const val big_u_value =
        "(?:\\\\U$hex_digit$hex_digit$hex_digit$hex_digit$hex_digit$hex_digit$hex_digit$hex_digit)"
    private const val little_u_value = "(?:\\\\u$hex_digit$hex_digit$hex_digit$hex_digit)"
    private const val hex_byte_value = "(?:\\\\x$hex_digit$hex_digit)"
    private const val octal_byte_value = "(?:\\\\$octal_digit$octal_digit$octal_digit)"
    private const val byte_value = "(?:$octal_byte_value|$hex_byte_value)"
    private const val unicode_value = "(?:$unicode_char|$little_u_value|$big_u_value|$escaped_char)"
    private const val rune_lit = "(?:'(?:$unicode_value|$byte_value)')"

    private const val interpreted_string_lit = "(?:\"(?:$unicode_value|$byte_value)*\")"
    private const val raw_string_lit = "(?:`[\\s\\S]*`)" // stack overflow: "(?:`(?:$unicode_char|$newline)*`)"
    private const val string_lit = "(?:$raw_string_lit|$interpreted_string_lit)"

    /**
     * Adds semi-colons to the lines of the specified input stream,
     * and writes the result to the specified output stream.
     *
     * @param inputStream the input stream to read from
     * @param outputStream the output stream to write to
     */
    fun colonize(inputStream: InputStream, outputStream: OutputStream) {
        BufferedInputStream(inputStream).use { ins ->
            BufferedOutputStream(outputStream).use { outs ->
                InputStreamReader(ins).use { ir ->
                    OutputStreamWriter(outs).use { or ->
                        colonize(ir, or)
                    }
                }
            }
        }
    }

    /**
     * Adds semi-colons to the lines of the specified reader,
     * and writes the result to the specified writer.
     *
     * @param reader the reader to read from
     * @param writer the writer to write to
     */
    fun colonize(reader: Reader, writer: Appendable) {
        BufferedReader(reader).use {
            val str = reader.readText()
            val tokens = InputTokenizer.tokenize(str)
            for(tokenLine in tokens) {
                val last = tokenLine.lastOrNull { it != "\n" }

                val addSemiColon = last != null && shouldEndWithSemicolon(last)

                tokenLine.dropLastWhile { it == "\n" }.forEach { writer.append(it) }
                if (addSemiColon) {
                    writer.append(";")
                }

                val eol = tokenLine.lastOrNull()
                if (eol == "\n") writer.appendln()
            }
        }

        // TODO: Insert a semi-colon before a closing ) or } in a statement context
    }

    /**
     * Whether the line should end with a semi-colon.
     *
     * @param lastToken the last token on the line
     * @return `true` to add a terminating semi-colon; otherwise, `false`
     */
    fun shouldEndWithSemicolon(lastToken: String): Boolean {
        // @formatter:off
        return isKeyword(lastToken)
            || isOperator(lastToken)
            || isIdentifier(lastToken)
            || isIntLit(lastToken)
            || isFloatLit(lastToken)
            || isStringLit(lastToken)
            || isRuneLit(lastToken)
            || isImaginaryLit(lastToken)
        // @formatter:on
    }

    fun isKeyword(token: String): Boolean =
        token in listOf("break", "continue", "fallthrough", "return")

    fun isOperator(token: String): Boolean =
        token in listOf("++", "--", ")", "]", "}")

    fun isIdentifier(token: String): Boolean =
        token.matches(Regex(identifier))

    fun isIntLit(token: String): Boolean =
        token.matches(Regex(int_lit))

    fun isFloatLit(token: String): Boolean =
        token.matches(Regex(float_lit))

    fun isStringLit(token: String): Boolean =
        token.matches(Regex(string_lit))

    fun isRuneLit(token: String): Boolean =
        token.matches(Regex(rune_lit))

    fun isImaginaryLit(token: String): Boolean =
        token.matches(Regex(imaginary_lit))

}
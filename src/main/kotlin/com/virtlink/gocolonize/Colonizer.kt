package com.virtlink.gocolonize

object Colonizer {

    private const val newline = "\\n"
    private const val unicode_char = "[^\\n]"
    private const val unicode_letter = "\\\\p{L}"
    private const val unicode_digit = "\\\\p{N}"
    private const val letter = "[${unicode_letter}_]"

    private const val identifier = "$letter[$unicode_digit${unicode_digit}_]*"

    private const val decimal_digit = "[0-9]"
    private const val binary_digit = "[0-1]"
    private const val octal_digit = "[0-7]"
    private const val hex_digit = "[0-9a-fA-F]"

    private const val decimal_digits = "(?:$decimal_digit(?:_?$decimal_digit)*)"
    private const val binary_digits = "(?:$binary_digit(?:_?$binary_digit)*)"
    private const val octal_digits = "(?:$octal_digit(?:_?$octal_digit)*)"
    private const val hex_digits = "(?:$hex_digit(?:_?$hex_digit)*)"

    private const val decimal_lit = "(?:0|[1-9](?:_?$decimal_digits))"
    private const val binary_lit = "(?:0[bB]_?$binary_digits)"
    private const val octal_lit = "(?:0[oO]?_?$octal_digits)"
    private const val hex_lit = "(?:0[xX]_?$hex_digits)"

    private const val decimal_exponent = "(?:[eE])[+\\-]?$decimal_digits)"
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
    private const val raw_string_lit = "(?:`(?:$unicode_char|$newline)*`)"
    private const val string_lit = "(?:$raw_string_lit|$interpreted_string_lit)"

    /**
     * Adds semi-colons to the specified line.
     *
     * @param line the line to colonize
     * @return the colonized line
     */
    fun colonizeLine(line: String): String {
        // TODO: Improved tokenization
        val lastToken = line.split(' ', '\t', '\r', '\n').lastOrNull() ?: return line

        // @formatter:off
        val terminateWithSemiColon =
            isKeyword(lastToken) ||
            isOperator(lastToken) ||
            isIdentifier(lastToken) ||
            isIntLit(lastToken) ||
            isFloatLit(lastToken) ||
            isStringLit(lastToken) ||
            isRuneLit(lastToken) ||
            isImaginaryLit(lastToken)
        // @formatter:on

        // TODO: Inserts a semi-colon before a closing ) or } in a statement context

        TODO()
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
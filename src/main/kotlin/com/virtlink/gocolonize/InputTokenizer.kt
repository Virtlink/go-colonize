package com.virtlink.gocolonize

/**
 * Stateful class for tokenizing input.
 */
object InputTokenizer {

    // Longer delimiters ordered before shorter delimiters that are its prefix
    private val delimiters = listOf(
        "/*", "*/", "//",
        "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", "&^=",
        "==", "!=", "<=", ">=", "||", "&&", "&ˆ", "<<", ">>", "<-", "->", "++", "--",
        ":=", "=", "|", "&", "^", "+", "-", "*", "/", "%",
        "...", ";", ",", ":", ".", "!",
        "{", "}", "(", ")", "<", ">", "[", "]",
        "\"", "'", "`",
        " ", "\t", "\r", "\n"
    )

    /**
     * Tokenizes the given input, producing a list of tokens per line.
     */
    fun tokenize(input: String): List<List<String>> {
        val lines = mutableListOf<List<String>>()
        var tokens = mutableListOf<String>()
        lines.add(tokens)

        var index = 0
        while (index < input.length) {
            val result = input.findAnyOf(delimiters, index)
            if (result == null) {
                // EOF
                tokens.add(input.substring(index))
                break
            }
            val (nextIndex, delimiter) = result
            if (nextIndex > index) {
                // Token between the previous delimiter and the next
                tokens.add(input.substring(index, nextIndex))
            }
            index = nextIndex

            when (delimiter) {
                "\n" -> {
                    // EOL
                    tokens.add(delimiter)
                    index += delimiter.length
                    // New line
                    tokens = mutableListOf()
                    lines.add(tokens)
                }
                "//" -> {
                    // Single-line comment
                    val eol = input.indexOf('\n', index)
                    if (eol >= 0) {
                        // Comment until EOL
                        assert(eol >= index)
                        tokens.add(input.substring(index, eol))
                        index = eol
                    } else if (eol == -1) {
                        // Comment until EOF
                        tokens.add(input.substring(index))
                        index = input.length
                    }
                }
                "/*" -> {
                    // Block comment
                    val commentEnd = input.indexOf("*/", index)
                    if (commentEnd >= 0) {
                        // Comment until */
                        assert(commentEnd >= index)
                        tokens.add(input.substring(index, commentEnd + 2))
                        index = commentEnd + 2
                    } else if (commentEnd == -1) {
                        // Comment not ended
                        tokens.add(input.substring(index))
                        index = input.length
                    }
                }
                "\"" -> {
                    // Interpreted string
                    var occurrence = input.findAnyOf(listOf("\\\\", "\\\"", "\"", "\n"), index + 1)
                    loop@ while (occurrence != null) {
                        val (occIndex, occStr) = occurrence
                        when (occStr) {
                            "\n" -> {
                                // String until newline
                                tokens.add(input.substring(index, occIndex))
                                index = occIndex
                            }
                            "\"" -> {
                                // String until "
                                assert(occIndex >= index)
                                tokens.add(input.substring(index, occIndex + 1))
                                index = occIndex + 1
                                break@loop
                            }
                            else -> {
                                // Escape sequence
                                occurrence = input.findAnyOf(listOf("\\\\", "\\\"", "\"", "\n"), occIndex + occStr.length)
                            }
                        }
                    }
                    if (occurrence == null) {
                        // String not ended
                        tokens.add(input.substring(index))
                        index = input.length
                    }
                }
                "`" -> {
                    // Raw string
                    val stringEnd = input.indexOf("`", index + 1)
                    if (stringEnd >= 0) {
                        // String until `
                        assert(stringEnd >= index)
                        tokens.add(input.substring(index, stringEnd + 1))
                        index = stringEnd + 1
                    } else if (stringEnd == -1) {
                        // String not ended
                        tokens.add(input.substring(index))
                        index = input.length
                    }
                }
                "'" -> {
                    // Rune
                    var occurrence = input.findAnyOf(listOf("\\\\", "\\'", "'", "\n"), index + 1)
                    loop@ while (occurrence != null) {
                        val (occIndex, occStr) = occurrence
                        when (occStr) {
                            "\n" -> {
                                // Rune until newline
                                tokens.add(input.substring(index, occIndex))
                                index = occIndex
                            }
                            "\"" -> {
                                // Rune until '
                                assert(occIndex >= index)
                                tokens.add(input.substring(index, occIndex + 1))
                                index = occIndex + 1
                                break@loop
                            }
                            else -> {
                                // Escape sequence
                                occurrence = input.findAnyOf(listOf("\\\\", "\\'", "'", "\n"), occIndex + occStr.length)
                            }
                        }
                    }
                    if (occurrence == null) {
                        // Rune not ended
                        tokens.add(input.substring(index))
                        index = input.length
                    }
                }
                else -> {
                    // Delimiter
                    tokens.add(delimiter)
                    index += delimiter.length
                }
            }
        }

        return lines

    }

}
package com.virtlink.gocolonize

/**
 * Stateful class for tokenizing input.
 */
class InputTokenizer {

    // Longer delimiters ordered before shorter delimiters that are its prefix
    private val delimiters = listOf(
        "/*", "*/", "//",
        "==", "!=", "<=", ">=", "||", "&&", "&ˆ", "<<", ">>", "<-", "->", "++", "--",
        ":=", "=", "|", "&", "^", "+", "-", "*", "/", "%",
        ";", ",", ":", "...", ".",
        "{", "}", "(", ")", "<", ">", "[", "]",
        "\"", "'", "`",
        " ", "\t", "\r", "\n"
    )

    private var state = State.Normal

    fun tokenizeLine(line: String): List<String> {
        return when (this.state) {
            State.Normal -> tokenizeNormalLine(line)
            State.RawString -> tokenizeRawString(line)
            State.CommentBlock -> tokenizeCommentBlock(line)
            else -> throw UnsupportedOperationException()
        }
    }

    private fun tokenizeRawString(line: String, startIndex: Int = 0): List<String> {
        val stringEnd = line.indexOf("`", startIndex)
        if (stringEnd == -1) return listOf(line)
        // Make a single token of the string, followed by the tokenized rest of the line
        state = State.Normal
        return listOf(line.substring(0, stringEnd + 1)) + tokenizeLine(line.substring(stringEnd + 1))
    }

    private fun tokenizeInterpretedString(line: String, startIndex: Int = 0): List<String> {
        var occurrence = line.findAnyOf(listOf("\\\\", "\\\"", "\""), startIndex)
        while (occurrence != null) {
            val (occIndex, occStr) = occurrence
            if (occStr != "\"") {
                occurrence = line.findAnyOf(listOf("\\\\", "\\\"", "\""), occIndex + occStr.length)
            } else {
                // Make a single token of the string, followed by the tokenized rest of the line
                state = State.Normal
                return listOf(line.substring(0, occIndex + 1)) + tokenizeLine(line.substring(occIndex + 1))
            }
        }
        return listOf(line)
    }

    private fun tokenizeCommentBlock(line: String, startIndex: Int = 0): List<String> {
        val commentEnd = line.indexOf("*/", startIndex)
        if (commentEnd == -1) return listOf(line)
        // Make a single token of the comment, followed by the tokenized rest of the line
        state = State.Normal
        return listOf(line.substring(0, commentEnd + 2)) + tokenizeLine(line.substring(commentEnd + 2))
    }

    private fun tokenizeNormalLine(line: String): List<String> {
        val tokens = mutableListOf<String>()

        var index = 0
        while (index < line.length) {
            val result = line.findAnyOf(delimiters, index)
            if (result == null) {
                tokens.add(line.substring(index))
                break
            }
            val (nextIndex, delimiter) = result
            if (nextIndex > index) tokens.add(line.substring(index, nextIndex))
            when (delimiter) {
                "//" -> {
                    // A single-line comment
                    tokens.add(line.substring(nextIndex))
                    return tokens
                }
                "/*" -> {
                    // Entering a block comment
                    state = State.CommentBlock
                    return tokens + tokenizeCommentBlock(line.substring(nextIndex), 2)
                }
                "\"" -> {
                    // Entering an interpreted string
                    return tokens + tokenizeInterpretedString(line.substring(nextIndex), 1)
                }
                "`" -> {
                    // Entering a raw string
                    state = State.RawString
                    return tokens + tokenizeRawString(line.substring(nextIndex), 1)
                }
                !in arrayOf(" ", "\t", "\r", "\n") -> {
                    // A non-whitespace token
                    tokens.add(delimiter)
                }
            }

            index = nextIndex + delimiter.length
        }

        return tokens
    }

    private enum class State {
        Normal,
        RawString,
        CommentBlock
    }

}
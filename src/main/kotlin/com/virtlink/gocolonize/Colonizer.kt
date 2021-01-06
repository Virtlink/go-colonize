package com.virtlink.gocolonize

import com.virtlink.gocolonize.parser.GoLexer
import com.virtlink.gocolonize.parser.GoParser
import com.virtlink.gocolonize.parser.GoParserBaseListener
import mu.KotlinLogging
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.*
import java.lang.Appendable
import java.nio.file.Path
import kotlin.jvm.Throws

/**
 * Adds semicolons to the given input.
 *
 * @property onError Indicates how parse errors are handled.
 */
class Colonizer(
    private val onParseError: OnParseError
) {

    private val log = KotlinLogging.logger {}

    /**
     * Adds semi-colons to the lines of the specified input stream,
     * and writes the result to the specified output stream.
     *
     * @param inputStream the input stream to read from
     * @param outputStream the output stream to write to
     * @throws FatalParseErrorException if a parse error was fatal
     */
    @Throws(FatalParseErrorException::class)
    fun colonize(inputStream: InputStream, outputStream: OutputStream) {
        val str = BufferedInputStream(inputStream).use {
            InputStreamReader(it).use { ir ->
                ir.readText()
            }
        }
        BufferedOutputStream(outputStream).use {
            OutputStreamWriter(it).use { ow ->
                colonize(str, ow, null)
            }
        }
    }

    /**
     * Adds semi-colons to the lines of the specified input stream,
     * and writes the result to the specified output stream.
     *
     * @param inputPath the input path; or `null` to input from STDIN
     * @param outputPath the output path; or `null` to output to STDOUT
     * @throws FatalParseErrorException if a parse error was fatal
     */
    @Throws(FatalParseErrorException::class)
    fun colonize(inputPath: Path?, outputPath: Path?) {
        val inputStream = if (inputPath != null) FileInputStream(inputPath.toFile()) else UncloseableInputStream(System.`in`)
        val str = inputStream.use { ins -> BufferedInputStream(ins).use { bins -> InputStreamReader(bins).use { ir ->
            ir.readText()
        } } }
        val outputStream = if (outputPath != null) FileOutputStream(outputPath.toFile()) else UncloseableOutputStream(System.out)
        outputStream.use { outs -> BufferedOutputStream(outs).use { bouts -> OutputStreamWriter(bouts).use { ow ->
            colonize(str, ow, inputPath)
        } } }
    }

    /**
     * Adds semi-colons to the lines of the specified reader,
     * and writes the result to the specified writer.
     *
     * @param str the string to read from
     * @param writer the writer to write to
     * @param inputPath the input path, if any; otherwise, `null`
     * @throws FatalParseErrorException if a parse error was fatal
     */
    @Throws(FatalParseErrorException::class)
    fun colonize(str: String, writer: Appendable, inputPath: Path?) {
        val input = CharStreams.fromString(str, inputPath?.toString() ?: IntStream.UNKNOWN_SOURCE_NAME)
        val lexer = GoLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = GoParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(ColonizeErrorListener(inputPath))
        parser.addParseListener(ColonizeSemicolonPrinter(writer, tokens))
        parser.sourceFile()
    }

    /**
     * Listens to the Go parser and prints the tokens that where seen, including whitespace and comments.
     * This class writes semicolons where they are expected but not found.
     *
     * @property writer The writer to write to.
     * @property tokens The token stream.
     */
    private class ColonizeSemicolonPrinter(
        private val writer: Appendable,
        private val tokens: CommonTokenStream,
    ) : GoParserBaseListener() {

        override fun exitEos(ctx: GoParser.EosContext) {
            // If the token is non-empty, return
            // It will be added by visitTerminal
            if (ctx.start.tokenIndex <= ctx.stop.tokenIndex && ctx.start.type != Token.EOF) return
            writer.append(';')
        }

        override fun visitErrorNode(node: ErrorNode) {
            appendNode(node)
        }

        override fun visitTerminal(node: TerminalNode) {
            appendNode(node)
        }

        private fun appendNode(node: TerminalNode) {
            val leadingWS = tokens.getHiddenTokensToLeft(node.symbol.tokenIndex) ?: emptyList()
            for (ws in leadingWS) {
                writer.append(ws.text)
            }

            // We skip EOF (-1) and invalid (0) tokens
            if (node.symbol.type <= 0) return
            writer.append(node.text)
        }

    }

    /**
     * Writes errors to the logger.
     *
     * @property path The path of the file being parsed; or `null` when unspecified.
     */
    private inner class ColonizeErrorListener(
        private val path: Path?,
    ) : BaseErrorListener() {

        /**
         * @throws FatalParseErrorException if a parse error was fatal
         */
        @Throws(FatalParseErrorException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            when (onParseError) {
                OnParseError.Fatal -> {
                    log.error(getErrorMessage(line, charPositionInLine, msg))
                    throw FatalParseErrorException(msg)
                }
                OnParseError.Warn -> log.warn(getErrorMessage(line, charPositionInLine, msg))
                OnParseError.Ignore -> /* Ignored */ Unit
            }
        }

        private fun getErrorMessage(
            line: Int,
            charPositionInLine: Int,
            msg: String
        ): String {
            return "${if (path != null) "$path:" else ""}$line:$charPositionInLine $msg"
        }
    }

}
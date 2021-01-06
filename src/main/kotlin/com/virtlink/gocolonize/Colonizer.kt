package com.virtlink.gocolonize

import com.virtlink.gocolonize.parser.GoLexer
import com.virtlink.gocolonize.parser.GoParser
import com.virtlink.gocolonize.parser.GoParserBaseListener
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode
import java.io.*
import java.lang.Appendable
import java.nio.file.Path

object Colonizer {

    /**
     * Adds semi-colons to the lines of the specified input stream,
     * and writes the result to the specified output stream.
     *
     * @param inputStream the input stream to read from
     * @param outputStream the output stream to write to
     */
    fun colonize(inputStream: InputStream, outputStream: OutputStream) {
        val str = BufferedInputStream(inputStream).use {
            InputStreamReader(it).use { ir ->
                ir.readText()
            }
        }
        BufferedOutputStream(outputStream).use {
            OutputStreamWriter(it).use { ow ->
                colonize(str, ow)
            }
        }
    }

    /**
     * Adds semi-colons to the lines of the specified input stream,
     * and writes the result to the specified output stream.
     *
     * @param inputStream the input path; or `null` to input from STDIN
     * @param outputPath the output path; or `null` to output to STDOUT
     */
    fun colonize(inputPath: Path?, outputPath: Path?) {
        val inputStream = if (inputPath != null) FileInputStream(inputPath.toFile()) else UncloseableInputStream(System.`in`)
        val str = inputStream.use { ins -> BufferedInputStream(ins).use { bins -> InputStreamReader(bins).use { ir ->
            ir.readText()
        } } }
        val outputStream = if (outputPath != null) FileOutputStream(outputPath.toFile()) else UncloseableOutputStream(System.out)
        outputStream.use { outs -> BufferedOutputStream(outs).use { bouts -> OutputStreamWriter(bouts).use { ow ->
            colonize(str, ow)
        } } }
    }

    /**
     * Adds semi-colons to the lines of the specified reader,
     * and writes the result to the specified writer.
     *
     * @param str the string to read from
     * @param writer the writer to write to
     */
    fun colonize(str: String, writer: Appendable) {
        val input = CharStreams.fromString(str)
        val lexer = GoLexer(input)
        val tokens = CommonTokenStream(lexer)
        val parser = GoParser(tokens)
        parser.addParseListener(ColonizeParseListener(writer, tokens))
        parser.sourceFile()
    }

    private class ColonizeParseListener(
        private val writer: Appendable,
        private val tokens: CommonTokenStream,
    ) : GoParserBaseListener() {

        override fun exitEos(ctx: GoParser.EosContext) {
            // If the token is non-empty, return
            // It will be added by visitTerminal
            if (ctx.start.tokenIndex <= ctx.stop.tokenIndex && ctx.start.type != Token.EOF) return
            writer.append(';')
        }

        override fun visitTerminal(node: TerminalNode) {
            val leadingWS = tokens.getHiddenTokensToLeft(node.symbol.tokenIndex) ?: emptyList()
            for (ws in leadingWS) {
                writer.append(ws.text)
            }

            // We skip EOF (-1) and invalid (0) tokens
            if (node.symbol.type <= 0) return
            writer.append(node.text)
        }

    }

}
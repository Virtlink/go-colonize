package com.virtlink.gocolonize

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import java.io.*

fun main(args: Array<String>) = FlcCommand()
    .main(args)


class FlcCommand : CliktCommand(help="Adds semi-colons to a Go file") {
    val input by argument(help="Input file; or - to read from STDIN")
    val output by option("-o", "--output", help="Output file (default writes to STDOUT)")

    override fun run() {
        val inputStream = if (input != "-") BufferedInputStream(FileInputStream(input)) else System.`in`
        val outputStream = if (output != null) BufferedOutputStream(FileOutputStream(output!!)) else System.out

        Colonizer.colonize(inputStream, outputStream)
    }
}

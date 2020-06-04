package com.virtlink.gocolonize

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument

fun main(args: Array<String>) = FlcCommand()
    .main(args)


class FlcCommand : CliktCommand(help="Adds semi-colons to a Go file") {
    val input by argument(help="Input file; or - to read from STDIN")

    override fun run() {
        println("Hello World!")
    }
}

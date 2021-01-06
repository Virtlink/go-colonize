package com.virtlink.gocolonize

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import java.nio.file.*
import kotlin.streams.toList

/**
 * The main entry point.
 *
 * @param args the command-line arguments
 */
fun main(args: Array<String>) = ColonizeCommand()
    .versionOption("${ PropertiesManager.version} (${ PropertiesManager.revision})") { version -> """
        ------------------------------------------------------------
        Go Colonize $version
        ------------------------------------------------------------

        Build time:   ${PropertiesManager.buildTime}
        Revision:     ${PropertiesManager.fullRevision}

        JVM:          ${property("java.version")} (${property("java.vm.vendor")} ${property("java.vm.version")})
        OS:           ${property("os.name")} ${property("os.version")} (${property("os.arch")})
        
        User:         ${property("user.name")}
        User home:    ${property("user.home")}
        Working dir:  ${property("user.dir")}
        
        ------------------------------------------------------------
        Copyright 2020-2021 Daniel A. A. Pelsmaeker
        Licensed under the Apache License, Version 2.0.

        This project includes source code from other projects:
        - Golang Antlr Grammar (BSD-3 license)
        """.trimIndent()
    }
    .main(args)

@Suppress("MemberVisibilityCanBePrivate")
class ColonizeCommand : CliktCommand(name="colonize", help="Adds semi-colons to a Go file") {
    // @formatter:off
    val inputs       by argument(help="Input files or directories; or none to read from STDIN").multiple().unique()
    val recursive    by option("-r", "--recursive", help="Recursively process the specified directories").flag()
    val outputFile   by option("-O", "--output-file", help="Output file", metavar = "FILE")
    val outputDir    by option("-o", "--output-dir", help="Output directory", metavar = "DIR")
    val outputPrefix by option("-p", "--output-prefix", help="Output filename prefix", metavar = "PREFIX")
    val outputSuffix by option("-s", "--output-suffix", help="Output filename suffix", metavar = "SUFFIX")
    val outputExt    by option("-e", "--output-ext", help="Output filename extension, incl dot", metavar = ".EXT")
    val quiet        by option("-q", "--quiet", help="Quiet mode").flag()
    val dryRun       by option("-n", "--dry-run", help="Shows what would happen").flag()
    // @formatter:on

    override fun run() {
        val files = gatherFiles()

        if (files.isEmpty() && inputs.isNotEmpty()) {
            eprintln("No files specified.")
            return
        }

        if (files.size > 1 && outputFile != null) {
            eprintln("Multiple input files specified, but one output file.")
            return
        }

        if (outputFile == null && outputDir == null && outputPrefix == null && outputSuffix == null && outputExt == null && files.isNotEmpty()) {
            eprintlnQuiet("Output to standard out")
        }

        if (files.isNotEmpty()) {
            for (file in files) {
                val outputPath = getOutputPath(file)
                if (dryRun) {
                    if (outputPath != null && Files.notExists(outputPath.parent)) {
                        println("Create ${outputPath.parent}")
                    }
                    println("$file -> ${outputPath ?: "STDOUT"}")
                } else {
                    ensureParentExists(outputPath)
                    eprintlnQuiet("$file...")
                    Colonizer.colonize(file, outputPath)
                }
            }
        } else {
            val outputPath = if (outputFile != null) Paths.get(outputFile!!) else null
            if (dryRun) {
                println("STDIN -> ${outputPath ?: "STDOUT"}")
            } else {
                Colonizer.colonize(null, outputPath)
            }
        }

        eprintlnQuiet("Done.")
    }

    /**
     * Ensures the parent directory of the specified path exists.
     *
     * @param path the path; or `null`
     */
    private fun ensureParentExists(path: Path?) {
        if (path == null) return
        val parent = path.parent
        Files.createDirectories(parent)
    }

    private val goFileMatcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.go")

    private fun eprintlnQuiet(msg: String) {
        if (quiet) return
        eprintln(msg)
    }

    private fun eprintln(msg: String) {
        System.err.println(msg)
        System.err.flush()
    }

    /**
     * Gathers the paths of all files to process.
     */
    private fun gatherFiles(): List<Path> {
        return inputs.map{ Paths.get(it) }.flatMap {
            if (Files.isDirectory(it)) {
                find(it, if (recursive) Int.MAX_VALUE else 1)
            } else {
                listOf(it)
            }
        }.distinct()
    }

    /**
     * Finds all files in the specified path.
     */
    private fun find(path: Path, depth: Int = Int.MAX_VALUE): List<Path> {
        return Files.find(path, depth, { p, a -> a.isRegularFile && goFileMatcher.matches(p) }).toList()
    }

    private fun getOutputPath(path: Path): Path? {
        if (outputFile != null) return Paths.get(outputFile!!)
        if (outputDir == null && outputPrefix == null && outputSuffix == null && outputExt == null) return null

        val (name, ext) = splitExtension(path.fileName.toString())

        val outputPath = if (outputDir != null) Paths.get(outputDir!!).resolve(Paths.get("").relativize(path)) else path
        return outputPath.resolveSibling( "${outputPrefix ?: ""}$name${outputSuffix ?: ""}${outputExt ?: ext}")
    }

    /**
     * Splits a filename into a name and an extension (including the dot)
     * When the filename starts with a dot, the starting dot is ignored.
     */
    private fun splitExtension(filename: String): Pair<String, String> {
        val extIndex = filename.indexOf('.', 1)
        if (extIndex < 1) return Pair(filename, "")
        return Pair(filename.substring(0, extIndex), filename.substring(extIndex))
    }
}

/**
 * Helper function that keys a key from the system properties.
 *
 * @param key the key to get
 * @param default the default value; or `null`
 * @return the value read from the properties; or the default value if not found
 */
fun property(key: String, default: String? = null): String? = System.getProperty(key, default)

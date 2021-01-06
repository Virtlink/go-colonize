package com.virtlink.gocolonize

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.enum
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import mu.KotlinLogging
import java.lang.IllegalStateException
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

/**
 * The main command.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ColonizeCommand : CliktCommand(name="colonize", help="Adds semi-colons to a Go file") {
    private val log = KotlinLogging.logger {}

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
    val onError      by option("-E", "--on-error", help="How parse errors are handled").enum<OnParseError>().default(OnParseError.Warn)
    // @formatter:on

    /** A set of directories that have been created. */
    private val createdDirs: MutableSet<Path> = mutableSetOf()

    /** Matches .go files. */
    private val goFileMatcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.go")

    override fun run() {
        setLogLevel()

        val files = gatherFiles()

        if (files.isEmpty() && inputs.isNotEmpty()) {
            log.error("No files specified.")
            return
        }

        if (files.size > 1 && outputFile != null) {
            log.error("Multiple input files specified, but one output file.")
            return
        }

        if (outputFile == null && outputDir == null && outputPrefix == null && outputSuffix == null && outputExt == null && files.isNotEmpty()) {
            log.info("Output to standard out")
        }

        try {
            if (files.isNotEmpty()) {
                for (file in files) {
                    val outputPath = getOutputPath(file)
                    ensureParentExists(outputPath, dryRun)
                    colonize(file, outputPath, dryRun)
                }
            } else {
                val outputPath = if (outputFile != null) Paths.get(outputFile!!) else null
                colonize(null, outputPath, dryRun)
            }
        } catch (ex: FatalParseErrorException) {
            // Exception was thrown to abort and exit. It has been logged already.
            log.warn("Aborted.")
            return
        } catch (ex: IllegalStateException) {
            log.error(ex.message, ex)
            return
        }

        log.info("Done.")
    }

    /**
     * Adds semicolons to the specified path.
     *
     * @param file the file to colonize; or `null` to input from STDIN
     * @param outputPath the output path; or `null` to output to STDOUT
     * @param dryRun whether this is a dry-run
     */
    private fun colonize(file: Path?, outputPath: Path?, dryRun: Boolean) {
        if (dryRun) {
            println("${file ?: "STDIN"} -> ${outputPath ?: "STDOUT"}")
        } else {
            log.info("${file ?: "STDIN"} -> ${outputPath ?: "STDOUT"}")
            Colonizer(onError).colonize(file, outputPath)
            log.debug("Done: ${file ?: "STDIN"} -> ${outputPath ?: "STDOUT"}")
        }
    }

    /**
     * Ensures the parent directory of the specified path exists.
     *
     * @param outputPath the output path; or `null` when output should be written to STDOUT
     * @param dryRun whether this is a dry-run
     */
    private fun ensureParentExists(outputPath: Path?, dryRun: Boolean) {
        if (outputPath == null) return
        val parent = outputPath.parent
        if (parent !in createdDirs && Files.notExists(parent)) {
            createdDirs.add(parent)
            if (!dryRun) {
                log.debug("Creating $parent...")
                Files.createDirectories(parent)
                log.info("Created $parent")
            } else {
                println("Create $parent")
            }
        } else if (!dryRun && Files.notExists(parent)) {
            throw IllegalStateException("Directory was created but no longer exists: $parent")
        }
    }

    /**
     * Sets the log level.
     */
    private fun setLogLevel() {
        val root: Logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        if (quiet) root.level = Level.WARN
        // If not quiet, we'll use the log level specified in logback.xml
    }

    /**
     * Gathers the paths of all files to process.
     *
     * @return a list of file paths found
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
     *
     * @param path the path to search
     * @param depth the maximum depth to search
     * @return a list of file paths found
     */
    private fun find(path: Path, depth: Int = Int.MAX_VALUE): List<Path> {
        return Files.find(path, depth, { p, a -> a.isRegularFile && goFileMatcher.matches(p) }).map { it.normalize() }.toList()
    }

    /**
     * Determines the output path for the given input path.
     *
     * @param inputPath the input path
     * @return the output path; or `null` when output should be written to STDOUT
     */
    private fun getOutputPath(inputPath: Path): Path? {
        if (outputFile != null) return Paths.get(outputFile!!)
        if (outputDir == null && outputPrefix == null && outputSuffix == null && outputExt == null) return null

        val (name, ext) = splitExtension(inputPath.fileName.toString())

        val outputPath = if (outputDir != null) Paths.get(outputDir!!).resolve(Paths.get("").relativize(inputPath)) else inputPath
        return outputPath.resolveSibling( "${outputPrefix ?: ""}$name${outputSuffix ?: ""}${outputExt ?: ext}").normalize()
    }

    /**
     * Splits a filename into a name and an extension (including the dot)
     * When the filename starts with a dot, the starting dot is ignored.
     *
     * @param filename the filename to split
     * @return a pair of the filename and the extension
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

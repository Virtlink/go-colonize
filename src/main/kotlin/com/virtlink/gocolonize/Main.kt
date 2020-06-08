package com.virtlink.gocolonize

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.*
import java.util.function.BiPredicate
import kotlin.streams.toList


fun main(args: Array<String>) = ColonizeCommand()
    .versionOption("1.0") { version ->
        // @formatter:off
        "colonize $version\n" +
        "\n" +
        "Copyright 2020 Daniel A. A. Pelsmaeker\n"+
        "\n"+
        "Licensed under the Apache License, Version 2.0 (the \"License\");\n"+
        "you may not use this file except in compliance with the License.\n"+
        "You may obtain a copy of the License at\n"+
        "\n"+
        "    http://www.apache.org/licenses/LICENSE-2.0\n"+
        "\n"+
        "Unless required by applicable law or agreed to in writing, software\n"+
        "distributed under the License is distributed on an \"AS IS\" BASIS,\n"+
        "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"+
        "See the License for the specific language governing permissions and\n"+
        "limitations under the License."
        // @formatter:on
    }
    .main(args)


class ColonizeCommand : CliktCommand(name="colonize", help="Adds semi-colons to a Go file") {
    val inputs by argument(help="Input files or directories; or none to read from STDIN").multiple().unique()
    val recursive by option("-r", "--recursive", help="Recursively process the specified directories").flag()
    val outputFile by option("-O", "--output-file", help="Output file", metavar = "FILE")
    val outputDir by option("-o", "--output-dir", help="Output directory", metavar = "DIR")
    val outputPrefix by option("-p", "--output-prefix", help="Output filename prefix", metavar = "PREFIX")
    val outputSuffix by option("-s", "--output-suffix", help="Output filename suffix", metavar = "SUFFIX")
    val outputExt by option("-e", "--output-ext", help="Output filename extension, incl dot", metavar = ".EXT")
    val quiet by option("-q", "--quiet", help="Quiet mode").flag()
    val list by option("-l", "--list", help="Lists what would happen").flag()

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
                if (list) {
                    println("$file -> ${outputPath ?: "STDOUT"}")
                } else {
                    BufferedInputStream(FileInputStream(file.toFile())).use { inputStream ->
                        val outputStream =
                            if (outputPath != null) BufferedOutputStream(FileOutputStream(outputPath.toFile())) else System.out

                        eprintlnQuiet("$file...")

                        Colonizer.colonize(inputStream, outputStream)

                        outputStream.flush()
                        if (outputPath != null) outputStream.close()
                    }
                }
            }
        } else {
            val inputStream = System.`in`
            val outputPath = if (outputFile != null) Paths.get(outputFile!!) else null
            if (list) {
                println("STDIN -> ${outputPath ?: "STDOUT"}")
            } else {
                val outputStream =
                    if (outputPath != null) BufferedOutputStream(FileOutputStream(outputPath.toFile())) else System.out

                Colonizer.colonize(inputStream, outputStream)

                outputStream.flush()
                if (outputPath != null)
                    outputStream.close()
            }
        }

        eprintlnQuiet("Done.")
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
        return Files.find(path, depth, BiPredicate { p, a -> a.isRegularFile && goFileMatcher.matches(p) }).toList()
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

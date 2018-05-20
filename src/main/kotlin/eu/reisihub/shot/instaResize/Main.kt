package eu.reisihub.shot.instaResize

import com.xenomachina.argparser.*
import java.awt.image.BufferedImage
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.streams.toList

object Main {

    private class CommandLineArgs(argParser: ArgParser) {
        val srcFolder by argParser.storing(
            "--source",
            "--src",
            "-s",
            help = "Source folder for images. Looking for a folder, whch contains JPG images",
            argName = "sourceFolder"
        ) { Paths.get(this)!! }.addValidator {
            Files.list(value)
                .filter { it.fileName.toString().let { it.endsWith("jpg", true) || it.endsWith("jpeg", true) } }
                .findAny().ifPresent {
                    throw SystemExitException("Folder $value does not contain JPG files...", -1)
                }
        }
        val targetFolder: Path by argParser.storing(
            "-o",
            "--out",
            "--outFolder",
            help = "Out folder for images. Can be a non-existant folder!",
            argName = "outFolder"
        ) { Paths.get(this) }.default(srcFolder.resolve("out"))

    }

    val executorService =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())


    val instagramSupportedRatios = AspectRatio(4, 5)..AspectRatio(191, 100)

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            CommandLineArgs(ArgParser(args)).run {
                Files.list(srcFolder).filter {
                    Files.isRegularFile(it) &&
                            it.fileName.toString().let { it.endsWith("jpg", true) || it.endsWith("jpeg", true) }
                }.sorted().toList().let { files ->
                    if (files.isEmpty())
                        throw IllegalStateException("No files to process!")
                    println("Loading initial image!")
                    files.first().readImage().aspectRatio.let {
                        if (it in instagramSupportedRatios)
                            it
                        else
                            findNextSupportedRatio(it)
                    }.let { finalRatio ->
                        println("Final ratio found: $finalRatio!")
                        println("Creating batch jobs!")
                        files.map { it to executorService.submit { getResizeJob(it, targetFolder, finalRatio) } }
                            .asSequence().map { (sourcePath, targetPathFuture) ->
                                targetPathFuture.get().also { println("Converted image $sourcePath. Stored at $it") }
                            }.count().let { totalImages ->
                                println("Finished converting $totalImages images!")
                            }

                    }
                }
            }
        } catch (e: ShowHelpException) {
            val help = StringWriter().apply { e.printUserMessage(this, "InstaResize CLI", 120) }.toString()
            println(help)
        } catch (e: MissingValueException) {
            println("${e.valueName} is a required parameter and missing!")
        }
    }

    private fun findNextSupportedRatio(sourceRatio: AspectRatio): AspectRatio = when {
        instagramSupportedRatios.start > sourceRatio -> instagramSupportedRatios.start
        instagramSupportedRatios.endInclusive < sourceRatio -> instagramSupportedRatios.endInclusive
        else -> sourceRatio
    }


    private fun Path.readImage(): BufferedImage = ImageIO.read(Files.newInputStream(this, StandardOpenOption.READ))

    private fun getResizeJob(fromImage: Path, toFolder: Path, targetAspectRatio: AspectRatio): () -> Path {
        TODO("Implement")
    }
}
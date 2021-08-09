package eu.reisihub.soft.instaResize

import com.xenomachina.argparser.*
import eu.reisihub.shot.*
import java.awt.Color
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.streams.toList

object Main {

    private class CommandLineArgs(argParser: ArgParser) {
        val srcFolder: Path by argParser.storing(
            "--source",
            "--src",
            "-s",
            help = "Source folder for images. Looking for a folder, whch contains JPG images",
            argName = "SOURCE_FOLDER"
        ) { Paths.get(this)!! }.addValidator {
            Files.list(value)
                .filter { it.fileName.toString().let { it.endsWith("jpg", true) || it.endsWith("jpeg", true) } }
                .findAny().ifAbsent {
                    throw SystemExitException("Folder $value does not contain JPG files...", -1)
                }
        }

        val targetFolder: Path by argParser.storing(
            "-o",
            "-t",
            "--out",
            "--target",
            help = "Target folder for images. Can be a non-existant folder!",
            argName = "TARGET_FOLDER"
        ) { Paths.get(this) }.default {
            srcFolder.resolve("out")
        }

        val color: Color by argParser.storing(
            "-c",
            "--color",
            "--colour",
            help = "Background color for generated image, defaults to BLACK",
            argName = "COLOR"
        ) {
            try {
                (Color::class.java.getField(this.uppercase()).get(null) as? Color)!!
            } catch (e: NoSuchFieldException) {
                println("Color not found, falling back to BLACK")
                Color.BLACK
            } catch (e: NullPointerException) {
                println("Color not found, falling back to BLACK")
                Color.BLACK
            }
        }
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
                        if (!Files.exists(targetFolder))
                            Files.createDirectories(targetFolder)
                        files.map {
                            it to executorService.submit(
                                getResizeJob(
                                    it,
                                    targetFolder,
                                    finalRatio,
                                    color
                                )
                            )
                        }
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
            println("${e.valueName} is a required parameter and is missing!")
        } finally {
            executorService.shutdownNow().isEmpty().let { empty ->
                if (!empty)
                    throw IllegalStateException("Unexpected batch jobs pending!")
            }
        }
    }

    private fun findNextSupportedRatio(sourceRatio: AspectRatio): AspectRatio = when {
        instagramSupportedRatios.start > sourceRatio -> instagramSupportedRatios.start
        instagramSupportedRatios.endInclusive < sourceRatio -> instagramSupportedRatios.endInclusive
        else -> sourceRatio
    }

    private fun getResizeJob(
        fromImage: Path,
        toFolder: Path,
        targetAspectRatio: AspectRatio,
        bgColor: Color
    ): () -> Path = {
        fromImage.readImage().let { image ->
            image.aspectRatio.let { imageRatio ->
                when {
                // increase width
                    imageRatio < targetAspectRatio -> {
                        (imageRatio.heightAspect.toDouble() / targetAspectRatio.heightAspect * targetAspectRatio.widthAspect).let { proposedWidthAspect ->
                            val desiredWidth =
                                Math.round(Math.ceil(proposedWidthAspect)).toInt() //Width is larger than before!
                            desiredWidth to imageRatio.heightAspect
                        }

                    }
                // increase height
                    imageRatio > targetAspectRatio -> {
                        (imageRatio.widthAspect.toDouble() / targetAspectRatio.widthAspect * targetAspectRatio.heightAspect).let { proposedHeightAspect ->
                            val desiredHeight =
                                Math.round(Math.ceil(proposedHeightAspect)).toInt() //Height is larger than before!
                            imageRatio.widthAspect to desiredHeight
                        }
                    }
                    else -> imageRatio.widthAspect to imageRatio.heightAspect
                }.let { (desiredWidth, desiredHeight) ->
                    //Desired values are >= current values
                    val xOffset = (desiredWidth - imageRatio.widthAspect) / 2
                    val yOffset = (desiredHeight - imageRatio.heightAspect) / 2
                    BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_RGB).let { targetImage ->
                        targetImage.createGraphics().apply {
                            paint = bgColor
                            fillRect(0, 0, desiredWidth, desiredHeight)
                            drawImage(
                                image,
                                //No scaling, just draw the image at x/y coordinates
                                AffineTransform(1f, 0f, 0f, 1f, xOffset.toFloat(), yOffset.toFloat()),
                                null
                            )
                        }
                        toFolder.resolve(fromImage.fileName).also { outFileName ->
                            targetImage.storeJPG(outFileName)
                        }
                    }
                }
            }
        }
    }
}

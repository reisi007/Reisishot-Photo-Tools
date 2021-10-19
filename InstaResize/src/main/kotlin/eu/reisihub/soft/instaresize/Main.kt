package eu.reisihub.soft.instaresize

import com.xenomachina.argparser.*
import eu.reisihub.shot.*
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Composite
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

object Main {

    private class CommandLineArgs(argParser: ArgParser) {
        val srcFolder: Path by argParser.storing(
            "--source",
            "--src",
            "-s",
            help = "Source folder for images. Looking for a folder, whch contains JPG images",
            argName = "SOURCE_FOLDER"
        ) { Paths.get(this) }.addValidator {
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

        val imageBackgroundFill: Float by argParser.storing(
            "-f",
            "--fill",
            help = "If image should also be shown as background fill (value is transparency)",
            argName = "IMAGE_BG_FILL"
        ) {
            val toFloat = toFloat()
            when {
                toFloat < 0 -> 0f
                toFloat > 1 -> 1f
                else -> toFloat
            }
        }

        val targetAspectRatio: AspectRatio? by argParser.storing(
            "-r",
            "--ratio",
            help = "Ratio W:H, which overrides the computed target aspect ratio",
            argName = "ASPECT_RATIO"
        ) {
            val (width, height) = split(":", limit = 2)
                .map { it.toInt() }
            AspectRatio(width, height)
        }.default(null)
    }


    private val executorService =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())


    private val instagramSupportedRatios = AspectRatio(4, 5)..AspectRatio(191, 100)

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            CommandLineArgs(ArgParser(args)).run {
                val files = listImageFiles()

                println("Loading initial image!")

                val finalRatio =
                    targetAspectRatio
                        ?.also { println("Taking aspect ratio $it from command line") }
                        ?: files.first().loadRatioFromFile()

                println("Creating batch jobs!")

                if (!Files.exists(targetFolder))
                    Files.createDirectories(targetFolder)

                val imageCount = files.map {
                    it to executorService.submit(
                        getResizeJob(
                            it,
                            targetFolder,
                            finalRatio,
                            color,
                            imageBackgroundFill
                        )
                    )
                }
                    .asSequence()
                    .map { (sourcePath, targetPathFuture) ->
                        targetPathFuture.get().also { println("Converted image $sourcePath. Stored at $it") }
                    }
                    .count()

                println("Finished converting $imageCount images!")

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

    private fun CommandLineArgs.listImageFiles(): List<Path> {
        val files = Files.list(srcFolder)
            .filter {
                Files.isRegularFile(it) &&
                        it.fileName.toString().let { fileName ->
                            fileName.endsWith("jpg", true) ||
                                    fileName.endsWith("jpeg", true)
                        }
            }
            .sorted()
            .toList()

        if (files.isEmpty())
            throw IllegalStateException("No files to process!")
        return files
    }

    private fun Path.loadRatioFromFile(): AspectRatio {
        val loadedRatio = readImage().aspectRatio
        val aspectRatio = if (loadedRatio in instagramSupportedRatios)
            loadedRatio
        else
            findNextSupportedRatio(loadedRatio)
        println("Final ratio found: $aspectRatio!")
        return aspectRatio
    }

    private fun findNextSupportedRatio(sourceRatio: AspectRatio): AspectRatio = when {
        instagramSupportedRatios.start > sourceRatio -> instagramSupportedRatios.start
        instagramSupportedRatios.endInclusive < sourceRatio -> instagramSupportedRatios.endInclusive
        else -> sourceRatio
    }

    private fun Graphics2D.withComposite(newComposite: Composite, action: (Graphics2D) -> Unit) {
        val old = composite;
        composite = newComposite
        action(this)
        composite = old
    }

    private fun getResizeJob(
        fromImage: Path,
        toFolder: Path,
        targetAspectRatio: AspectRatio,
        bgColor: Color,
        imageBackgroundFill: Float
    ): () -> Path = {
        val image = fromImage.readImage()
        val aspectRatio = image.aspectRatio
        val (desiredWidth, desiredHeight) = computeImageRatio(aspectRatio, targetAspectRatio)

        //Desired values are >= current values
        val xOffset = (desiredWidth - aspectRatio.widthAspect) / 2.toFloat()
        val yOffset = (desiredHeight - aspectRatio.heightAspect) / 2.toFloat()

        val widthScaleFactor = desiredWidth / aspectRatio.widthAspect.toFloat()
        val heightScaleFactor = desiredHeight / aspectRatio.heightAspect.toFloat()
        val scaleFactor = max(widthScaleFactor, heightScaleFactor)

        val targetImage = BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_ARGB)

        with(targetImage.createGraphics()) {
            paint = bgColor
            fillRect(
                0,
                0,
                desiredWidth,
                desiredHeight
            )

            if (imageBackgroundFill > 0) {
                withComposite(
                    AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER,
                        imageBackgroundFill
                    )
                ) {
                    drawImage(
                        image,
                        AffineTransform(
                            scaleFactor,
                            0f,
                            0f,
                            scaleFactor,
                            -desiredWidth * (scaleFactor - widthScaleFactor) / 2,
                            -desiredHeight * (scaleFactor - heightScaleFactor) / 2
                        ),
                        null
                    )
                }
            }

            drawImage(
                image,
                //No scaling, just draw the image at x/y coordinates
                AffineTransform(
                    1f,
                    0f,
                    0f,
                    1f,
                    xOffset,
                    yOffset
                ),
                null
            )
        }

        val outFileName = toFolder.resolve(fromImage.fileName)
        targetImage.storeJPG(outFileName)
        outFileName
    }

    private fun computeImageRatio(
        imageRatio: AspectRatio,
        targetAspectRatio: AspectRatio
    ) = when {
        // increase width
        imageRatio < targetAspectRatio -> {
            (imageRatio.heightAspect.toDouble() / targetAspectRatio.heightAspect * targetAspectRatio.widthAspect).let { proposedWidthAspect ->
                val desiredWidth =
                    ceil(proposedWidthAspect).roundToInt() //Width is larger than before!
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
    }
}

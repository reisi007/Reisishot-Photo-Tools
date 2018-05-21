package eu.reisihub.soft.watermarking

import com.xenomachina.argparser.ArgParser
import eu.reisihub.shot.readImage
import java.awt.AlphaComposite
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.streams.toList

private class Args(private val argParser: ArgParser) {
    val settings by argParser.storing(
        "-f", "-s", "--file", "--settings", "-i", "--input",
        help = "Helper file", argName = "SETTINGS_FILE_JSON"
    ) { Paths.get(this).let { WatermarkSettings.load(it) } }
}

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        Args(ArgParser(args)).run { settings }.let { settings ->
            println(settings)
            println("Preparing Watermarking....")
            if (!Files.exists(settings.targetPath))
                Files.createDirectories(settings.targetPath)
            Files.list(settings.srcPath)
                .filter { it.fileName.toString().let { it.endsWith("jpg", true) || it.endsWith("jpeg", true) } }
                .limit(1).toList().let { images ->
                    if (images.isEmpty())
                        throw IllegalStateException("No images found!")

                    println("${images.size} images found!")

                    println("Preparing Watermark image....")

                    settings.watermarkImagePath.readImage().let {
                        AffineTransform().apply {
                            scale(settings.watermarkScale, settings.watermarkScale)

                        }.let {
                            AffineTransformOp(it, AffineTransformOp.TYPE_BICUBIC)
                        }.let { op ->
                            BufferedImage(
                                (settings.watermarkScale * it.width).toInt(),
                                (settings.watermarkScale * it.height).toInt(),
                                BufferedImage.TYPE_INT_ARGB
                            ).apply {
                                op.filter(it, this)
                            }.let {
                                BufferedImage(it.width, it.height, BufferedImage.TYPE_INT_ARGB).apply {
                                    createGraphics().also {
                                        AlphaComposite.getInstance(
                                            AlphaComposite.SRC_OVER,
                                            settings.watermarkTransparency
                                        )
                                        it.drawImage(this, 0, 0, null)
                                    }
                                }
                            }.let { watermark ->
                                val executorService =
                                    Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
                                try {
                                    images.map {
                                        WatermarkImageTask.create(
                                            it,
                                            settings.targetPath,
                                            settings.imageSize,
                                            watermark,
                                            settings.watermarkX,
                                            settings.watermarkY,
                                            settings.orientation
                                        ).let {
                                            executorService.submit(it)
                                        }
                                    }.asSequence().map { it.get().apply { println("Finished converting $this!") } }
                                        .count().let { count ->
                                            println("Finished adding watermark to $count images!")
                                            println("Images can be found at ${settings.targetPath}")
                                        }

                                } finally {
                                    executorService.shutdownNow().isEmpty()
                                        .let { success -> if (!success) throw IllegalStateException("Not all tasks completed in time!") }
                                }

                            }
                        }
                    }
                }
        }
    }
}
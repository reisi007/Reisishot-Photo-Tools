package eu.reisihub.soft.watermarking

import com.xenomachina.argparser.ArgParser
import eu.reisihub.shot.measured
import eu.reisihub.shot.readImage
import net.coobird.thumbnailator.Thumbnails
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.streams.toList

private class Args(argParser: ArgParser) {
    val settings by argParser.storing(
        "-f", "-s", "--file", "--settings", "-i", "--input",
        help = "Helper file", argName = "SETTINGS_FILE_JSON"
    ) { Paths.get(this).let { WatermarkSettings.load(it) } }
}

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        measured {
            Args(ArgParser(args)).run { settings }.let { settings ->
                println(settings)
                println("Preparing Watermarking....")
                if (!Files.exists(settings.targetPath))
                    Files.createDirectories(settings.targetPath)
                Files.list(settings.srcPath)
                    .filter { it.fileName.toString().let { it.endsWith("jpg", true) || it.endsWith("jpeg", true) } }
                    .toList().let { images ->
                        if (images.isEmpty())
                            throw IllegalStateException("No images found!")

                        println("${images.size} images found!")

                        println("Preparing Watermark image....")

                        settings.watermarkImagePath.readImage().let {
                            Thumbnails.of(it).scale(settings.watermarkScale).asBufferedImage()
                        }.let { watermark ->
                            println("Starting watermarking!")
                            val executorService =
                                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
                            try {
                                images.map {
                                    WatermarkUtils.create(
                                        it,
                                        settings.targetPath,
                                        settings.imageSize,
                                        ImageWatermark(
                                            watermark,
                                            settings.watermarkX,
                                            settings.watermarkY,
                                            settings.watermarkTransparency,
                                            settings.orientation
                                        )
                                    ).let {
                                        executorService.submit(it)
                                    }
                                }.asSequence().map {
                                    it.get()
                                }.map {
                                    it.apply { println("Finished converting $this!") }
                                }
                                    .count().let { count ->
                                        println("Finished adding watermark to $count images!")
                                        println("Images can be found at ${settings.targetPath}")
                                    }
                            } finally {
                                executorService.shutdownNow()
                                    .size.let { failedTasks -> if (failedTasks > 0) println("Not all tasks completed in time! [$failedTasks]") }
                            }
                        }
                    }
            }
        }.let { ms -> println("Execution time: $ms ms") }
    }
}

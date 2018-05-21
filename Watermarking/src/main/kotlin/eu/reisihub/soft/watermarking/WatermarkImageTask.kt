package eu.reisihub.soft.watermarking

import eu.reisihub.shot.readImage
import eu.reisihub.shot.withChild
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.imageio.ImageIO

object WatermarkImageTask {

    fun create(
        imagePath: Path,
        outFolder: Path,
        scaleToMaxSize: Int,
        watermarkImage: BufferedImage,
        watermarkX: Int,
        watermarkY: Int,
        watermarkOrientation: Orientation
    ): () -> Path = {
        imagePath.readImage().let {
            //Transform
            it
        }.let { watermarkedImage ->

            (outFolder withChild imagePath.fileName).apply {
                Files.newOutputStream(
                    this,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                ).use {
                    ImageIO.write(watermarkedImage, "JPEG", it)
                }
            }
        }
    }
}
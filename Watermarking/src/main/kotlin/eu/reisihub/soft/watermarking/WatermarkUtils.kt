package eu.reisihub.soft.watermarking

import eu.reisihub.shot.readImage
import eu.reisihub.shot.storeJPG
import eu.reisihub.shot.withChild
import net.coobird.thumbnailator.Thumbnails
import java.nio.file.Files
import java.nio.file.Path

object WatermarkUtils {

    fun create(
        imagePath: Path,
        outFolder: Path,
        scaleToMaxSize: Int,
        vararg watermarkSettings: IWatermark
    ): () -> Path = {
        (outFolder withChild imagePath.fileName).also { outPath ->
            if (Files.exists(outPath))
                return@also
            imagePath.readImage().let {
                //Scale to size
                Thumbnails.of(it).size(scaleToMaxSize, scaleToMaxSize).asBufferedImage()

            }.let {
                //Apply watermarks
                val data = it to it.createGraphics()
                watermarkSettings.forEach {
                    it.applyTo(data)
                }
                it
            }.storeJPG(outPath)
        }
    }
}
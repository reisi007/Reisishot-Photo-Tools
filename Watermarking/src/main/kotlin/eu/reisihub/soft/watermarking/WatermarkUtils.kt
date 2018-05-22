package eu.reisihub.soft.watermarking

import eu.reisihub.shot.readImage
import eu.reisihub.shot.withChild
import net.coobird.thumbnailator.Thumbnails
import java.nio.file.Path

object WatermarkUtils {

    fun create(
        imagePath: Path,
        outFolder: Path,
        scaleToMaxSize: Int,
        vararg watermarkSettings: IWatermark
    ): () -> Path = {
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
        }.let { watermarkedImage ->
            (outFolder withChild imagePath.fileName).apply {
                Thumbnails.of(watermarkedImage).scale(1.0).outputQuality(1.0).toFile(this.toFile())
            }
        }
    }
}
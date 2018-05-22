package eu.reisihub.soft.watermarking;

import eu.reisihub.shot.toRoundedInt
import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.image.BufferedImage

data class ImageWatermark(
    private val watermarkImage: BufferedImage,
    private val watermarkX: Int,
    private val watermarkY: Int,
    private val watermarkTransparency: Float,
    private val watermarkOrientation: IWatermark.Orientation
) : IWatermark {
    override fun applyTo(data: Pair<BufferedImage, Graphics2D>) {
        calculateImagePosition(
            watermarkOrientation,
            watermarkImage,
            data.first
        ).let { (x, y) -> x + watermarkX to y + watermarkY }
            .let { (finX, finY) ->
                data.second.apply {
                    val oldC = composite
                    composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, watermarkTransparency)
                    drawImage(watermarkImage, finX, finY, null)
                    composite = oldC
                }
            }
    }

    private fun calculateImagePosition(
        orientation: IWatermark.Orientation,
        watermarkImage: BufferedImage,
        baseImage: BufferedImage
    ): Pair<Int, Int> {
        val wOff = baseImage.width * (1 / 16)
        val hOff = baseImage.height * (1 / 16)
        val x = when (orientation) {
            IWatermark.Orientation.BOTTOM_LEFT, IWatermark.Orientation.MIDDLE_LEFT, IWatermark.Orientation.TOP_LEFT -> wOff
            IWatermark.Orientation.BOTTOM_CENTER, IWatermark.Orientation.MIDDLE_CENTER, IWatermark.Orientation.TOP_CENTER -> ((baseImage.width / 2f) - (watermarkImage.width / 2f)).toRoundedInt()
            IWatermark.Orientation.BOTTOM_RIGHT, IWatermark.Orientation.MIDDLE_RIGHT, IWatermark.Orientation.TOP_RIGHT -> baseImage.width - wOff - watermarkImage.width
        }
        val y = when (orientation) {
            IWatermark.Orientation.TOP_RIGHT, IWatermark.Orientation.TOP_CENTER, IWatermark.Orientation.TOP_LEFT -> hOff
            IWatermark.Orientation.MIDDLE_RIGHT, IWatermark.Orientation.MIDDLE_CENTER, IWatermark.Orientation.MIDDLE_LEFT -> ((baseImage.height / 2f) - (watermarkImage.height / 2f)).toRoundedInt()
            IWatermark.Orientation.BOTTOM_RIGHT, IWatermark.Orientation.BOTTOM_CENTER, IWatermark.Orientation.BOTTOM_LEFT -> baseImage.height - hOff - watermarkImage.height
        }
        return x to y
    }
}
package eu.reisihub.soft.watermarking

import java.awt.Graphics2D
import java.awt.image.BufferedImage

interface IWatermark {
    fun applyTo(data: Pair<BufferedImage, Graphics2D>)

    enum class Orientation {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }
}
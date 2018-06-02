package eu.reisihub.shot

import net.coobird.thumbnailator.Thumbnails
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.*
import javax.swing.ImageIcon

fun Optional<*>.ifAbsent(block: () -> Unit) {
    if (!isPresent)
        block()
}

infix fun Path.withChild(other: String): Path = resolve(other)
infix fun Path.withChild(other: Path): Path = resolve(other)

fun Path.readImage(): BufferedImage =
    ImageIcon(toUri().toURL()).let {
        BufferedImage(it.iconWidth, it.iconHeight, BufferedImage.TYPE_INT_ARGB).apply {
            it.paintIcon(null, createGraphics(), 0, 0)
        }
    }

fun Double.toRoundedInt() = Math.round(this).toInt()
fun Float.toRoundedInt() = Math.round(this)

fun measured(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    val end = System.currentTimeMillis()
    return end - start
}

fun BufferedImage.storeJPG(p: Path) = Thumbnails.of(this).scale(1.0).outputQuality(1.0).toFile(p.toFile())
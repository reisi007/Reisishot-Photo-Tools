package eu.reisihub.shot

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream

fun Optional<*>.ifAbsent(block: () -> Unit) {
    if (!isPresent)
        block()
}

infix fun Path.withChild(other: String): Path = resolve(other)
infix fun Path.withChild(other: Path): Path = resolve(other)

fun Path.readImage(): BufferedImage = Files.newInputStream(this, StandardOpenOption.READ).buffered().use {
    var nStream: ImageInputStream? = null
    var nReader: ImageReader? = null
    synchronized(System.err) {
        nStream = ImageIO.createImageInputStream(it) ?: throw IIOException("Can't create an ImageInputStream!")

        val iter = ImageIO.getImageReaders(nStream)
        if (!iter.hasNext()) {
            throw IIOException("No image nReader found!")
        }
        nReader = iter.next()
    }

    nStream!!.let { stream ->
        nReader!!.let { reader ->
            reader.setInput(stream, true, true)
            try {
                return reader.read(0, null)
            } finally {
                reader.dispose()
                stream.close()
            }
        }
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
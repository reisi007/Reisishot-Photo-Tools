package eu.reisihub.shot

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import javax.imageio.ImageIO

fun Optional<*>.ifAbsent(block: () -> Unit) {
    if (!isPresent)
        block()
}

infix fun Path.withChild(other: String): Path = resolve(other)
infix fun Path.withChild(other: Path): Path = resolve(other)

fun Path.readImage(): BufferedImage = ImageIO.read(Files.newInputStream(this, StandardOpenOption.READ))
package eu.reisihub.soft.watermarking

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

fun main(args: Array<String>) {
    Scanner(System.`in`).let { console ->
        WatermarkSettings().let {
            println("Images source folder:")
            it.copy(nSrcPath = console.readPath())
        }.let {
            println("Target image folder:")
            it.copy(nTargetPath = console.readPath())
        }.let {
            println("Target size of the longest image side:")
            it.copy(imageSize = console.readPositiveInt())
        }.let {
            println("Watermark orientation (input number):")
            println()
            val possibleItems = Orientation.values()
            possibleItems.forEachIndexed { i, orientation ->
                println("$i: $orientation")
            }
            var selectedIndex = -1
            while (selectedIndex !in 0 until possibleItems.size)
                selectedIndex = console.nextInt()

            it.copy(orientation = possibleItems[selectedIndex])
        }.let {
            println("X-Watermark offset in pixel:")
            it.copy(watermarkX = console.nextInt())
        }.let {
            println("Y-Watermark offset in pixel:")
            it.copy(watermarkY = console.nextInt())
        }.let {
            println("Watermark image scale:")
            it.copy(watermarkScale = console.readPositiveDouble())
        }.let {
            println("Watermark image path:")
            it.copy(nWatermarkImagePath = console.readPath())
        }.also { settings ->
            println("Store settings to:")
            console.readPath().let {
                settings.store(it)
            }
        }
    }
}

private fun Scanner.readPositiveInt(): Int {
    while (true)
        try {
            return nextInt().also {
                if (it <= 0)
                    throw IllegalStateException("Number must be greater than 0, is $it")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
}

private fun Scanner.readPositiveDouble(): Double {
    while (true)
        try {
            return nextDouble().also {
                if (it <= 0)
                    throw IllegalStateException("Number must be greater than 0, is $it")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
}

private fun Scanner.readPath(): Path {
    while (true)
        try {
            return Paths.get(readLine())
        } catch (e: Exception) {
            e.printStackTrace()
        }
}
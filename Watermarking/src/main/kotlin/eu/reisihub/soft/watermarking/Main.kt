package eu.reisihub.soft.watermarking

import com.xenomachina.argparser.ArgParser
import java.nio.file.Paths

private class Args(private val argParser: ArgParser) {
    val settings by argParser.storing(
        "-f", "-s", "--file", "--settings", "-i", "--input",
        help = "Helper file", argName = "SETTINGS_FILE_JSON"
    ) { Paths.get(this).let { WatermarkSettings.load(it) } }
}

object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        Args(ArgParser(args)).run { settings }.let { settings ->
            println(settings)
        }
    }
}
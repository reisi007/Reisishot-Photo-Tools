package pictures.reisishot.previews.extract

import com.xenomachina.argparser.*
import eu.reisihub.shot.ifAbsent
import eu.reisihub.shot.withChild
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

object Main {

    private class CommandLineArgs(argParser: ArgParser) {
        val srcFolder: Path by argParser.storing(
            "--source",
            "--src",
            "-s",
            help = "Source folder for images. Looking for a folder, whch contains JPG images",
            argName = "SOURCE_FOLDER"
        ) { Paths.get(this) }.addValidator {
            Files.list(value)
                .filter { it.fileName.toString().let { it.endsWith("jpg", true) || it.endsWith("jpeg", true) } }
                .findAny().ifAbsent {
                    throw SystemExitException("Folder $value does not contain JPG files...", -1)
                }
        }

        val targetFolder: Path by argParser.storing(
            "-o",
            "-t",
            "--out",
            "--target",
            help = "Target folder for images. Can be a non-existant folder!",
            argName = "TARGET_FOLDER"
        ) { Paths.get(this) }.default {
            srcFolder.resolve("out")
        }

        operator fun component1() = srcFolder
        operator fun component2() = targetFolder
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            CommandLineArgs(ArgParser(args)).let { (src, internalTarget) ->
                val target = internalTarget withChild UUID.randomUUID().toString()
                createExtractImageJob(src, target, ImageType.PreviewImage).forEach { it() }
                createExtractImageJob(src, target withChild "thumbnails", ImageType.ThumbnailImage).forEach { it() }
            }
        } catch (e: ShowHelpException) {
            val help = StringWriter().apply { e.printUserMessage(this, "InstaResize CLI", 120) }.toString()
            println(help)
        } catch (e: MissingValueException) {
            println("${e.valueName} is a required parameter and is missing!")
        }
    }

    private fun createExtractImageJob(
        src: Path,
        target: Path,
        imageType: ImageType
    ) = sequenceOf(
        createExtractJob(src, target, imageType),
        createMetadataCopyJob(src, target),
        createMetadataJsonJob(target)
    )

    enum class ImageType {
        PreviewImage,
        ThumbnailImage
    }

    private fun createExtractJob(inFolder: Path, outFolder: Path, type: ImageType): () -> Unit {
        return {
            job("extracting previews") {
                "exiftool -b -$type -w \"$outFolder/%f.jpg\" -ext cr3 $inFolder".executeInCmd()
            }
        }
    }

    private fun createMetadataCopyJob(inFolder: Path, outFolder: Path): () -> Unit {
        return {
            job("copying metadata to extracted files") {
                "exiftool -overwrite_original -TagsFromFile \"$inFolder/%f.cr3\" $outFolder/ -ext jpg".executeInCmd()
            }
        }
    }

    private fun createMetadataJsonJob(outFolder: Path): () -> Unit {
        return {
            job("Creating Metadata file") {
                "exiftool -j -Orientation -ImageWidth -ImageHeight -Filename -ext jpg  \"$outFolder\" > \"$outFolder/meta.json\"".executeInCmd()
            }
        }
    }

    private fun String.executeInCmd() {
        val exitCode = ProcessBuilder("cmd.exe", "/c", this)
            .inheritIO()
            .start()
            .waitFor()

        if (exitCode != 0)
            throw IllegalStateException("Exit-Code of \"$this\" is $exitCode")
    }

    private fun job(title: String, action: () -> Unit) {
        println(" == Start $title ==")
        println()
        action()
        println()
        println(" == End $title ==")
        println()
    }
}

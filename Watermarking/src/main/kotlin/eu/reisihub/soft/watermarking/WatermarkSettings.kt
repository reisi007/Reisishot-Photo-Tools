package eu.reisihub.soft.watermarking

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import eu.reisihub.shot.withChild
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

private val JSON by lazy(
    GsonBuilder().registerTypeHierarchyAdapter(
        Path::class.java,
        object : JsonDeserializer<Path>, JsonSerializer<Path> {
            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Path =
                Paths.get(json.asString)


            override fun serialize(src: Path, typeOfSrc: Type, context: JsonSerializationContext): JsonElement =
                JsonPrimitive(src.toString())
        }
    )::create)

enum class Orientation {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

data class WatermarkSettings(
    @SerializedName("srcPath")
    private var nSrcPath: Path? = null,
    @SerializedName("targetPath")
    private var nTargetPath: Path? = null,
    @SerializedName("watermarkImagePath")
    private var nWatermarkImagePath: Path? = null,
    val imageSize: Int = 0,
    val watermarkScale: Double = 1.0,
    val watermarkX: Int = 0,
    val watermarkY: Int = 0,
    val watermarkTransparency: Float = 0.55f,
    val orientation: Orientation = Orientation.MIDDLE_CENTER
) {
    val srcPath
        get() = nSrcPath!!
    val targetPath
        get() = srcPath withChild "wout"

    val watermarkImagePath
        get() = nWatermarkImagePath!!

    fun store(file: Path) {
        Files.newBufferedWriter(
            file,
            StandardCharsets.UTF_8,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.CREATE
        ).use {
            JSON.toJson(
                this,
                it
            )
        }
    }

    companion object {
        fun load(path: Path): WatermarkSettings =
            JSON.fromJson(Files.newBufferedReader(path, StandardCharsets.UTF_8), WatermarkSettings::class.java)
    }
}
package eu.reisihub.sync

import com.adobe.internal.xmp.XMPConst
import com.adobe.internal.xmp.XMPMeta
import com.adobe.internal.xmp.XMPMetaFactory
import eu.reisihub.shot.withChild
import eu.reisihub.sync.data.Metadata
import eu.reisihub.sync.data.Person
import eu.reisihub.sync.db.WaitlistPersons
import eu.reisihub.sync.db.WaitlistPictureComments
import eu.reisihub.sync.db.WaitlistPictureRatings
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import java.math.BigDecimal
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.*
import kotlin.math.roundToInt


fun syncRatingsAndComments(metadataFolder: Path, folderId: String): List<SyncStatus> {
    val filesDirectory = metadataFolder.withChild(folderId)
        .readText()
        .let { Path(it.trim()) }

    val rating = loadRatings(folderId)
    val comment = loadComments(folderId)

    return sequenceOf(*comment.keys.toTypedArray(), *rating.keys.toTypedArray())
        .distinct()
        .map {
            filesDirectory.withChild(it).sync(
                Metadata(
                    comment.getOrDefault(it, emptyMap()),
                    rating.getOrDefault(it, emptyMap())
                )
            )
        }.toList()
}

private fun Path.sync(metadata: Metadata): SyncStatus {
    val xmpFile = parent withChild fileName.toString().substringBeforeLast(".") + ".xmp"
    return xmpFile.let {
        val xmp = it.loadOrCreateXmpFile()
        xmp.setProperty(XMPConst.NS_XMP, "Rating", metadata.avgRating.roundToInt().toString())
        xmp.setProperty(
            XMPConst.NS_XMP,
            "MetadataDate",
            ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        )
        xmp.setLocalizedText(
            XMPConst.NS_DC,
            "description",
            "x-default",
            "x-default",
            metadata.buildComment()
        )
        xmp.store(xmpFile)
        SyncStatus.Success
    }

}

private fun loadComments(folderId: String): Map<String, Map<Person, String>> {
    val destination = mutableMapOf<String, MutableMap<Person, String>>()
    WaitlistPersons.innerJoin(WaitlistPictureComments)
        .select {
            WaitlistPictureComments.folder.eq(folderId)
        }
        .forEach {
            val filename = it[WaitlistPictureComments.filename]
            val person = Person(it[WaitlistPersons.firstname], it[WaitlistPersons.lastname])
            val comment = it[WaitlistPictureComments.comment]

            destination.computeIfAbsent(filename) { mutableMapOf() }[person] = comment
        }
    return destination
}

private fun loadRatings(folderId: String): Map<String, Map<Person, BigDecimal>> {
    val destination = mutableMapOf<String, MutableMap<Person, BigDecimal>>()
    WaitlistPersons.innerJoin(WaitlistPictureRatings)
        .select(WaitlistPictureRatings.folder.eq(folderId))
        .forEach {
            val filename = it[WaitlistPictureRatings.filename]
            val person = Person(it[WaitlistPersons.firstname], it[WaitlistPersons.lastname])
            val rating = it[WaitlistPictureRatings.rating]

            destination.computeIfAbsent(filename) { mutableMapOf() }[person] = rating
        }
    return destination
}

fun Path.loadOrCreateXmpFile(): XMPMeta = if (exists())
    inputStream().use {
        XMPMetaFactory.parse(it)
    }
else XMPMetaFactory.create()

fun XMPMeta.store(file: Path) {
    file.outputStream(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE).use {
        XMPMetaFactory.serialize(this, it)
    }

}

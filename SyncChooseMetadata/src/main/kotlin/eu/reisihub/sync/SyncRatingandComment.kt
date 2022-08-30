package eu.reisihub.sync

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
import kotlin.io.path.Path
import kotlin.io.path.readText

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
    println(fileName to metadata)
    // TODO load / update | create / write XMP files
    return SyncStatus.Success
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


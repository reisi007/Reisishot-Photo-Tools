package eu.reisihub.sync

import eu.reisihub.sync.db.WaitlistPictureAccess
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths
import java.util.*

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val (propertyFilename, metadataFolderString) = args
        val metadataFolder = Paths.get(metadataFolderString)
        Properties().apply {
            propertyFilename
                .let { Main::class.java.classLoader.getResourceAsStream("$it.properties") }
                ?.use { load(it) }
                ?: throw IllegalStateException("Property file not found...")
        }
            .useDbConnection {
                val foldersToSync = WaitlistPictureAccess.slice(WaitlistPictureAccess.folder)
                    .selectAll()
                    .withDistinct()
                    .map { it[WaitlistPictureAccess.folder] }
                    .toSortedSet()
                runBlocking {
                    println("Sync results")
                    println()
                    foldersToSync.map {
                        it to async {
                            syncRatingsAndComments(metadataFolder, it)
                        }
                    }.map { (folder, syncJob) -> folder to syncJob.await() }
                        .forEach { (id, status) ->
                            print("$id ➔ ")
                            val errors = status.mapNotNull { it as? SyncStatus.Error }
                            println(if (errors.isEmpty()) "Success" else errors.toString())
                        }
                }
            }
    }
}

fun Properties.useDbConnection(action: Transaction.() -> Unit) {
    val db = Database.connect(
        url = getProperty("jdbc.url"),
        driver = "org.mariadb.jdbc.Driver",
        user = getProperty("jdbc.user"),
        password = getProperty("jdbc.pwd")
    )
    transaction(db, action)
}

package eu.reisihub.sync

import eu.reisihub.sync.db.WaitlistPictureAccess
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object Main {
    @JvmStatic
    fun main(args: Array<String>) = Properties().apply {
        (args.firstOrNull() ?: throw IllegalStateException("Need property filename..."))
            .let { Main::class.java.classLoader.getResource("$it.properties") }
            ?.openStream()
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
                    it to async { syncRatingsAndComments(it) }
                }.map { (folder, syncJob) -> folder to syncJob.await() }
                    .forEach { (id, status) ->
                        print("$id ➔ ")
                        println(
                            when (status) {
                                is SyncStatus.Error -> status.message
                                SyncStatus.Success -> "Success"
                            }
                        )
                    }
            }
        }
}

private suspend fun Transaction.syncRatingsAndComments(folderId: String): SyncStatus {
    // TODO perform sync
    return SyncStatus.Success
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

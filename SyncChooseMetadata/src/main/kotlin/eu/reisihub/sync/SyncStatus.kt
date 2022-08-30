package eu.reisihub.sync

import java.nio.file.Path

sealed class SyncStatus {
    object Success : SyncStatus()
    class Error(val file: Path, val message: String) : SyncStatus()
}

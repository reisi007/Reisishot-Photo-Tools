package eu.reisihub.sync

sealed class SyncStatus {
    object Success : SyncStatus()
    class Error(val message: String) : SyncStatus()
}

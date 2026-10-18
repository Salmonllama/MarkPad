package io.salmonllama.MarkPad.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long,
    val sourceUri: String? = null,
    val fileSource: FileSource = FileSource.INTERNAL,
    val sourceFileName: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long? = null
) {
    val isExternalFile: Boolean
        get() = !sourceUri.isNullOrBlank() && fileSource != FileSource.INTERNAL

    companion object {
        const val MAX_PINNED_NOTES = 3
    }
}

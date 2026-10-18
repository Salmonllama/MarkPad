package io.salmonllama.MarkPad.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import io.salmonllama.MarkPad.model.FileSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExternalFilePayload(
    val uri: String,
    val displayName: String,
    val content: String,
    val detectedSource: FileSource
)

interface ExternalFileGateway {
    suspend fun readFile(uriString: String, preferredSource: FileSource? = null): Result<ExternalFilePayload>
    suspend fun writeFile(uriString: String, content: String): Result<Unit>
}

class InMemoryExternalFileGateway : ExternalFileGateway {
    private val files = mutableMapOf<String, ExternalFilePayload>()
    private val writtenContents = mutableMapOf<String, String>()
    private val writeFailures = mutableSetOf<String>()

    fun registerFile(uri: String, payload: ExternalFilePayload) {
        files[uri] = payload
    }

    fun getWrittenContent(uri: String): String? = writtenContents[uri]

    fun setWriteFailure(uri: String, shouldFail: Boolean) {
        if (shouldFail) {
            writeFailures.add(uri)
        } else {
            writeFailures.remove(uri)
        }
    }

    override suspend fun readFile(
        uriString: String,
        preferredSource: FileSource?
    ): Result<ExternalFilePayload> {
        val existing = files[uriString]
            ?: return Result.failure(IllegalArgumentException("File not found for URI: $uriString"))
        val resolvedSource = if (preferredSource != null && preferredSource != FileSource.INTERNAL) {
            preferredSource
        } else {
            existing.detectedSource
        }
        return Result.success(existing.copy(detectedSource = resolvedSource))
    }

    override suspend fun writeFile(uriString: String, content: String): Result<Unit> {
        if (writeFailures.contains(uriString)) {
            return Result.failure( SecurityException("External source is read-only: $uriString"))
        }
        writtenContents[uriString] = content
        val existing = files[uriString]
        if (existing != null) {
            files[uriString] = existing.copy(content = content)
        }
        return Result.success(Unit)
    }
}

class AndroidExternalFileGateway(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExternalFileGateway {

    override suspend fun readFile(
        uriString: String,
        preferredSource: FileSource?
    ): Result<ExternalFilePayload> = withContext(ioDispatcher) {
        runCatching {
            val uri = Uri.parse(uriString)
            tryTakePersistablePermission(uri)

            val displayName = resolveDisplayName(uri)
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Unable to open input stream for $uriString")

            val detectedSource = if (preferredSource != null && preferredSource != FileSource.INTERNAL) {
                preferredSource
            } else {
                FileSource.fromUriAuthority(uri.authority, uri.path)
            }

            ExternalFilePayload(
                uri = uriString,
                displayName = displayName,
                content = content,
                detectedSource = detectedSource
            )
        }
    }

    override suspend fun writeFile(uriString: String, content: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val uri = Uri.parse(uriString)
            val outputStream = try {
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (_: Exception) {
                context.contentResolver.openOutputStream(uri)
            } ?: throw IllegalStateException("Unable to open output stream for $uriString")

            outputStream.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }

    private fun tryTakePersistablePermission(uri: Uri) {
        val resolver = context.contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            try {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // One-time intent grant (e.g., some external ACTION_VIEW providers); ignore
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        val name = cursor.getString(index)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        }
        val segment = uri.lastPathSegment ?: "Untitled.md"
        return segment.substringAfterLast('/').substringAfterLast(':').ifBlank { "Untitled.md" }
    }
}

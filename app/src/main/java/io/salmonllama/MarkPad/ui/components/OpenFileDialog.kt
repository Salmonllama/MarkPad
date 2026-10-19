package io.salmonllama.MarkPad.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract

/**
 * SAF ActivityResultContract for opening Markdown documents directly via the system file picker.
 * Configures appropriate Markdown MIME types, persistable URI permissions, and default initial directory.
 */
class OpenMarkdownDocumentContract : ActivityResultContract<Unit?, Uri?>() {
    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "text/markdown",
                    "text/x-markdown",
                    "text/plain",
                    "application/octet-stream"
                )
            )
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            val defaultInitialUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Documents"
            )
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, defaultInitialUri)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
    }
}

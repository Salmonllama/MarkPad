package io.salmonllama.MarkPad.model

enum class FileSource(
    val displayName: String,
    val shortBadge: String,
    val subtitle: String
) {
    INTERNAL(
        displayName = "MarkPad Scratchpad",
        shortBadge = "Internal",
        subtitle = "Stored in MarkPad local database"
    ),
    LOCAL_COMPUTER(
        displayName = "Local Computer",
        shortBadge = "Local",
        subtitle = "Browse Markdown files in local device storage & Documents"
    ),
    LINUX_ENVIRONMENT(
        displayName = "Linux Environment",
        shortBadge = "Linux",
        subtitle = "Browse files in the Linux VM & shared development workspace"
    ),
    GOOGLE_DRIVE(
        displayName = "Google Drive",
        shortBadge = "Drive",
        subtitle = "Open Markdown documents from your Google Drive"
    );

    companion object {
        fun fromNameOrDefault(name: String?): FileSource {
            if (name.isNullOrBlank()) return INTERNAL
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: INTERNAL
        }

        fun fromUriAuthority(authority: String?, path: String?): FileSource {
            val authLower = authority?.lowercase().orEmpty()
            val pathLower = path?.lowercase().orEmpty()

            return when {
                authLower.contains("com.google.android.apps.docs") ||
                    authLower.contains("drive") -> GOOGLE_DRIVE

                authLower.contains("terminal") ||
                    authLower.contains("linux") ||
                    authLower.contains("cros") ||
                    pathLower.contains("documents/source") ||
                    pathLower.contains("documents%2fsource") ||
                    pathLower.contains("/mnt/shared") ||
                    pathLower.contains("/home/") -> LINUX_ENVIRONMENT

                else -> LOCAL_COMPUTER
            }
        }
    }
}

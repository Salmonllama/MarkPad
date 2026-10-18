package io.salmonllama.MarkPad.export

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String
) {
    PDF(
        extension = "pdf",
        mimeType = "application/pdf",
        displayName = "PDF Document (.pdf)"
    ),
    DOCX(
        extension = "docx",
        mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        displayName = "Word Document (.docx)"
    ),
    MARKDOWN(
        extension = "md",
        mimeType = "text/markdown",
        displayName = "Markdown (.md)"
    )
}

package io.salmonllama.MarkPad.export

import io.salmonllama.MarkPad.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class PdfExporterTest {

    @Test
    fun export_delegatesToDocumentWriter() {
        var recordedNote: Note? = null

        val exporter = PdfExporter { note, out ->
            recordedNote = note
            out.write("PDF_TABLE_OUTPUT".toByteArray())
        }

        val testNote = Note(
            id = "pdf-test",
            title = "Table PDF",
            content = """
                | Service | Tier | Cost |
                | :--- | :---: | ---: |
                | Compute | Enterprise | ${'$'}150 |
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val output = ByteArrayOutputStream()
        exporter.export(testNote, output)

        assertEquals("pdf-test", recordedNote?.id)
        assertTrue(recordedNote?.content?.contains("Service") == true)
        assertEquals("PDF_TABLE_OUTPUT", String(output.toByteArray()))
    }

    @Test
    fun buildRenderOperationsSummary_coversFullGfmAstAndInlineSpans() {
        val gfmNote = Note(
            id = "pdf-gfm",
            title = "GFM PDF Spec",
            content = """
                # Main Title
                ##### Subheading H5
                1. Ordered item one
                  - Nested bullet
                  - [x] Nested completed task
                > [!TIP]
                > Pro tip for developers.
                >> Deep blockquote
                ~~~bash
                echo "hello"
                ~~~
                <details open>
                <summary>Collapsible Section</summary>
                Nested paragraph with ~~strike~~, [Docs](https://example.com), <kbd>Esc</kbd>, x<sup>2</sup>, H<sub>2</sub>O, ==mark==, ![Diagram](https://example.com/d.png), and ref[^n1].
                </details>
                ---
                [^n1]: Footnote body.
            """.trimIndent(),
            updatedAt = System.currentTimeMillis()
        )

        val ops = PdfExporter.buildRenderOperationsSummary(gfmNote)

        assertTrue(ops.contains("H1:Main Title"))
        assertTrue(ops.contains("H5:Subheading H5"))
        assertTrue(ops.contains("ORDERED[indent=0,num=1]:Ordered item one"))
        assertTrue(ops.contains("BULLET[indent=1]:Nested bullet"))
        assertTrue(ops.contains("TASK[indent=1,checked=true]:Nested completed task"))
        assertTrue(ops.contains("ALERT[type=TIP]:Pro tip for developers."))
        assertTrue(ops.contains("QUOTE[depth=2]:Deep blockquote"))
        assertTrue(ops.contains("CODEBLOCK[lang=bash]:echo \"hello\""))
        assertTrue(ops.contains("DETAILS[open=true]:Collapsible Section"))
        assertTrue(ops.any { it.startsWith("P:") && it.contains("S(strike)") && it.contains("LINK(Docs->https://example.com)") && it.contains("KBD(Esc)") && it.contains("SUP(2)") && it.contains("SUB(2)") && it.contains("HL(mark)") && it.contains("IMG(Diagram->https://example.com/d.png)") && it.contains("FNREF(n1)") })
        assertTrue(ops.contains("HR"))
        assertTrue(ops.contains("FOOTNOTE[id=n1]:Footnote body."))
    }
}

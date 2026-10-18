package io.salmonllama.MarkPad.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusFooterTest {

    @Test
    fun formatScratchpadCount_zeroNotes_returnsPlural() {
        assertEquals("0 scratchpads", formatScratchpadCount(0))
    }

    @Test
    fun formatScratchpadCount_singleNote_returnsSingular() {
        assertEquals("1 scratchpad", formatScratchpadCount(1))
    }

    @Test
    fun formatScratchpadCount_multipleNotes_returnsPlural() {
        assertEquals("2 scratchpads", formatScratchpadCount(2))
        assertEquals("10 scratchpads", formatScratchpadCount(10))
    }
}

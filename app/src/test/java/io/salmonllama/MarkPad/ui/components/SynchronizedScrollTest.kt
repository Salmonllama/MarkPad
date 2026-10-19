package io.salmonllama.MarkPad.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SynchronizedScrollTest {

    @Test
    fun `calculateScrollFraction calculates proportional float accurately`() {
        assertEquals(0.0f, SynchronizedScrollHelper.calculateScrollFraction(0, 1000), 0.0001f)
        assertEquals(0.5f, SynchronizedScrollHelper.calculateScrollFraction(500, 1000), 0.0001f)
        assertEquals(1.0f, SynchronizedScrollHelper.calculateScrollFraction(1000, 1000), 0.0001f)
        assertEquals(0.25f, SynchronizedScrollHelper.calculateScrollFraction(250, 1000), 0.0001f)
    }

    @Test
    fun `calculateScrollFraction clamps out of bounds values and handles zero or negative max`() {
        assertEquals(0.0f, SynchronizedScrollHelper.calculateScrollFraction(-50, 1000), 0.0001f)
        assertEquals(1.0f, SynchronizedScrollHelper.calculateScrollFraction(1500, 1000), 0.0001f)
        assertEquals(0.0f, SynchronizedScrollHelper.calculateScrollFraction(100, 0), 0.0001f)
        assertEquals(0.0f, SynchronizedScrollHelper.calculateScrollFraction(100, -10), 0.0001f)
    }

    @Test
    fun `calculateTargetScroll maps fraction to target maximum correctly`() {
        assertEquals(0, SynchronizedScrollHelper.calculateTargetScroll(0.0f, 2000))
        assertEquals(1000, SynchronizedScrollHelper.calculateTargetScroll(0.5f, 2000))
        assertEquals(2000, SynchronizedScrollHelper.calculateTargetScroll(1.0f, 2000))
        assertEquals(500, SynchronizedScrollHelper.calculateTargetScroll(0.25f, 2000))
        assertEquals(333, SynchronizedScrollHelper.calculateTargetScroll(0.3333f, 1000))
    }

    @Test
    fun `calculateTargetScroll handles edge cases with zero target max or clamped fractions`() {
        assertEquals(0, SynchronizedScrollHelper.calculateTargetScroll(0.5f, 0))
        assertEquals(0, SynchronizedScrollHelper.calculateTargetScroll(0.5f, -100))
        assertEquals(0, SynchronizedScrollHelper.calculateTargetScroll(-0.5f, 1000))
        assertEquals(1000, SynchronizedScrollHelper.calculateTargetScroll(1.5f, 1000))
    }

    @Test
    fun `calculateSynchronizedTarget converts source offset to proportional target offset`() {
        // Standard 1:2 ratio (source max 1000, target max 2000)
        assertEquals(0, SynchronizedScrollHelper.calculateSynchronizedTarget(0, 1000, 2000))
        assertEquals(500, SynchronizedScrollHelper.calculateSynchronizedTarget(250, 1000, 2000))
        assertEquals(1000, SynchronizedScrollHelper.calculateSynchronizedTarget(500, 1000, 2000))
        assertEquals(2000, SynchronizedScrollHelper.calculateSynchronizedTarget(1000, 1000, 2000))

        // Reverse 2:1 ratio (source max 2000, target max 1000)
        assertEquals(500, SynchronizedScrollHelper.calculateSynchronizedTarget(1000, 2000, 1000))
    }

    @Test
    fun `calculateSynchronizedTarget returns zero when bounds are invalid`() {
        assertEquals(0, SynchronizedScrollHelper.calculateSynchronizedTarget(100, 0, 1000))
        assertEquals(0, SynchronizedScrollHelper.calculateSynchronizedTarget(100, 1000, 0))
        assertEquals(0, SynchronizedScrollHelper.calculateSynchronizedTarget(100, 0, 0))
        assertEquals(0, SynchronizedScrollHelper.calculateSynchronizedTarget(100, -500, 1000))
    }

    @Test
    fun `driver state machine transitions properly and prevents mutual cancellation`() {
        // Initially idle
        val idle = ScrollDriver.NONE
        assertTrue(SynchronizedScrollHelper.canAcquireDriver(idle, ScrollDriver.EDITOR))
        assertTrue(SynchronizedScrollHelper.canAcquireDriver(idle, ScrollDriver.PREVIEW))

        // Editor takes control
        val editorActive = ScrollDriver.EDITOR
        assertTrue(SynchronizedScrollHelper.canAcquireDriver(editorActive, ScrollDriver.EDITOR))
        assertFalse(SynchronizedScrollHelper.canAcquireDriver(editorActive, ScrollDriver.PREVIEW))

        // Releasing wrong driver does nothing
        val unchanged = SynchronizedScrollHelper.releaseDriver(editorActive, ScrollDriver.PREVIEW)
        assertEquals(ScrollDriver.EDITOR, unchanged)

        // Releasing active driver resets to NONE
        val released = SynchronizedScrollHelper.releaseDriver(editorActive, ScrollDriver.EDITOR)
        assertEquals(ScrollDriver.NONE, released)

        // Preview takes control
        val previewActive = ScrollDriver.PREVIEW
        assertTrue(SynchronizedScrollHelper.canAcquireDriver(previewActive, ScrollDriver.PREVIEW))
        assertFalse(SynchronizedScrollHelper.canAcquireDriver(previewActive, ScrollDriver.EDITOR))

        val releasedPreview = SynchronizedScrollHelper.releaseDriver(previewActive, ScrollDriver.PREVIEW)
        assertEquals(ScrollDriver.NONE, releasedPreview)
    }

    @Test
    fun `initial note open alignment ensures both panes sync to matching location`() {
        // Scenario 1: New note opened at top (initial cursor = 0)
        val initialScroll = 0
        val editorMax = 800
        val previewMax = 1200
        val previewTarget = SynchronizedScrollHelper.calculateSynchronizedTarget(initialScroll, editorMax, previewMax)
        assertEquals(0, previewTarget)

        // Scenario 2: Existing note opened with saved cursor location (e.g. at 25% down the document)
        val editorCursorScroll = 200
        val synchronizedPreviewTarget = SynchronizedScrollHelper.calculateSynchronizedTarget(editorCursorScroll, editorMax, previewMax)
        assertEquals(300, synchronizedPreviewTarget)

        // Scenario 3: Short note where neither pane has scrollable content
        val shortNoteTarget = SynchronizedScrollHelper.calculateSynchronizedTarget(0, 0, 0)
        assertEquals(0, shortNoteTarget)
    }
}

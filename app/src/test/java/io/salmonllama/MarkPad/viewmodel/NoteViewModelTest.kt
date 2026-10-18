package io.salmonllama.MarkPad.viewmodel

import io.salmonllama.MarkPad.data.InMemoryNoteRepository
import io.salmonllama.MarkPad.data.SeedNotes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import io.salmonllama.MarkPad.data.InMemoryUserPreferencesRepository
import io.salmonllama.MarkPad.data.UserPreferencesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryNoteRepository
    private lateinit var preferencesRepository: InMemoryUserPreferencesRepository
    private lateinit var viewModel: NoteViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryNoteRepository(initialNotes = SeedNotes.items)
        preferencesRepository = InMemoryUserPreferencesRepository()
        viewModel = NoteViewModel(
            repository = repository,
            preferencesRepository = preferencesRepository,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsSeedNotes() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Ready but was $state", state is NoteUiState.Ready)
        val ready = state as NoteUiState.Ready
        assertEquals(3, ready.notes.size)
        assertEquals("meeting-retro", ready.selectedNoteId)
        assertNotNull(ready.selectedNote)
        assertTrue(ready.isPreviewVisible)
        assertEquals(SaveStatus.Saved, ready.saveStatus)
        assertEquals(ready.selectedNote?.content, ready.previewMarkdown)
    }

    @Test
    fun selectNote_updatesSelectedNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.selectNote("cli-cheatsheet")
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("cli-cheatsheet", ready.selectedNoteId)
        assertEquals("Developer CLI & Tooling Cheat Sheet", ready.selectedNote?.title)
        assertEquals(ready.selectedNote?.content, ready.previewMarkdown)
    }

    @Test
    fun togglePreview_updatesVisibility() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.togglePreview()
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertFalse(ready.isPreviewVisible)

        viewModel.togglePreview()
        advanceUntilIdle()

        val toggledBack = viewModel.uiState.value as NoteUiState.Ready
        assertTrue(toggledBack.isPreviewVisible)
    }

    @Test
    fun searchNotes_filtersByTitleOrContent() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.updateSearchQuery("Gradle")
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertTrue(ready.filteredNotes.any { it.title.contains("CLI") || it.content.contains("Gradle") })
    }

    @Test
    fun createNewNote_addsNoteAndSelectsIt() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.createNewNote()
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(4, ready.notes.size)
        assertTrue(ready.selectedNote?.title?.contains("New Scratchpad") == true)
        assertEquals(ready.selectedNote?.content, ready.previewMarkdown)
    }

    @Test
    fun onContentChanged_immediatelyUpdatesNoteContentAndCounters() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val updatedContent = "# Immediate Edits\n\nNew typed content."
        viewModel.onContentChanged(updatedContent)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(updatedContent, ready.selectedNote?.content)
        assertEquals(6, ready.wordCount) // Immediate word count update: ["#", "Immediate", "Edits", "New", "typed", "content."]
        assertEquals(updatedContent.length, ready.charCount)
        assertEquals(SaveStatus.Saved, ready.saveStatus)
        assertEquals(updatedContent, ready.previewMarkdown)
    }

    @Test
    fun onContentChanged_debouncesPreviewMarkdown() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val newContent = "# Rapid Typing\n\nDraft content..."
        viewModel.onContentChanged(newContent)

        // Advance before debounce expires (100ms < 150ms)
        advanceTimeBy(100)
        val intermediateState = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(SaveStatus.Saving, intermediateState.saveStatus)

        // Advance past debounce (total > 150ms)
        advanceTimeBy(100)
        advanceUntilIdle()

        val finalState = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(SaveStatus.Saved, finalState.saveStatus)
        assertEquals(newContent, finalState.previewMarkdown)
    }

    @Test
    fun openAndDismissExportDialog_togglesState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertFalse("Dialog initially closed", ready.isExportDialogOpen)

        viewModel.openExportDialog()
        advanceUntilIdle()

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertTrue("Dialog should be open", ready.isExportDialogOpen)

        viewModel.dismissExportDialog()
        advanceUntilIdle()

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertFalse("Dialog should be closed", ready.isExportDialogOpen)
    }

    @Test
    fun exportNoteToStream_exportsContentAndSetsNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.openExportDialog()
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        val note = ready.selectedNote!!
        val outputStream = java.io.ByteArrayOutputStream()

        viewModel.exportNoteToStream(
            note = note,
            format = io.salmonllama.MarkPad.export.ExportFormat.MARKDOWN,
            outputStream = outputStream
        )
        // Advance time enough for the export job to complete, but before notification auto-clears (delay 2500ms)
        advanceTimeBy(100)

        val afterExport = viewModel.uiState.value as NoteUiState.Ready
        assertFalse("Dialog closed after export", afterExport.isExportDialogOpen)
        assertEquals(note.content, outputStream.toString("UTF-8"))
        assertTrue(
            "Status message contains success notification",
            afterExport.statusMessage?.contains("successfully") == true
        )

        // Advance past delay(2500) to confirm notification clears
        advanceTimeBy(3000)
        val clearedState = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(null, clearedState.statusMessage)
    }

    @Test
    fun initialState_restoresLastOpenedNoteIdAndCursorFromPreferences() = runTest {
        val customPrefs = InMemoryUserPreferencesRepository()
        customPrefs.setLastOpenedNoteId("cli-cheatsheet")
        customPrefs.setLastCursorPosition("cli-cheatsheet", 42)

        val vm = NoteViewModel(
            repository = repository,
            preferencesRepository = customPrefs,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("State should be Ready but was $state", state is NoteUiState.Ready)
        val ready = state as NoteUiState.Ready
        assertEquals("cli-cheatsheet", ready.selectedNoteId)
        assertEquals(42, ready.cursorPosition)
        assertEquals(ready.selectedNote?.content, ready.previewMarkdown)
    }

    @Test
    fun initialState_fallsBackGracefullyIfLastNoteDoesNotExist() = runTest {
        val customPrefs = InMemoryUserPreferencesRepository()
        customPrefs.setLastOpenedNoteId("non-existent-note-id")

        val vm = NoteViewModel(
            repository = repository,
            preferencesRepository = customPrefs,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("State should be Ready but was $state", state is NoteUiState.Ready)
        val ready = state as NoteUiState.Ready
        assertEquals("meeting-retro", ready.selectedNoteId)
        assertEquals(0, ready.cursorPosition)
    }

    @Test
    fun selectNote_persistsLastOpenedNoteIdAndRestoresCursor() = runTest {
        preferencesRepository.setLastCursorPosition("feature-checklist", 25)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.selectNote("feature-checklist")
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("feature-checklist", ready.selectedNoteId)
        assertEquals(25, ready.cursorPosition)
        assertEquals("feature-checklist", preferencesRepository.getLastOpenedNoteId())
    }

    @Test
    fun onCursorPositionChanged_persistsCursorForCurrentNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onCursorPositionChanged(88)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(88, ready.cursorPosition)
        assertEquals(88, preferencesRepository.getLastCursorPosition("meeting-retro"))
    }

    @Test
    fun createNewNote_persistsAsLastOpenedAndResetsCursor() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.createNewNote()
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        val newId = ready.selectedNoteId
        assertNotNull(newId)
        assertEquals(0, ready.cursorPosition)
        assertEquals(newId, preferencesRepository.getLastOpenedNoteId())
        assertEquals(0, preferencesRepository.getLastCursorPosition(newId!!))
    }

    @Test
    fun onContentChanged_rapidTyping_updatesStatsImmediatelyWhileDebouncingSave() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Rapid keystroke 1
        viewModel.onContentChanged("# H")
        advanceTimeBy(50)
        var state = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("# H", state.selectedNote?.content)
        assertEquals(2, state.wordCount)
        assertEquals(3, state.charCount)
        assertEquals(SaveStatus.Saving, state.saveStatus)

        // Rapid keystroke 2 (<150ms since last key)
        viewModel.onContentChanged("# He")
        advanceTimeBy(50)
        state = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("# He", state.selectedNote?.content)
        assertEquals(2, state.wordCount)
        assertEquals(4, state.charCount)
        assertEquals(SaveStatus.Saving, state.saveStatus)

        // Rapid keystroke 3 (<150ms since last key)
        viewModel.onContentChanged("# Hello")
        advanceTimeBy(50)
        state = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("# Hello", state.selectedNote?.content)
        assertEquals(2, state.wordCount)
        assertEquals(7, state.charCount)
        assertEquals(SaveStatus.Saving, state.saveStatus)

        // Typing pause: advance past debounce threshold (150ms)
        advanceTimeBy(200)
        advanceUntilIdle()

        state = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(SaveStatus.Saved, state.saveStatus)
        assertEquals("# Hello", state.selectedNote?.content)
        assertEquals("# Hello", state.previewMarkdown)
    }

    @Test
    fun selectNote_flushesPendingDraftOfPreviousNoteImmediately() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initialNoteId = (viewModel.uiState.value as NoteUiState.Ready).selectedNoteId!!
        val unsavedContent = "# Fast typing before switch"
        viewModel.onContentChanged(unsavedContent)

        // Advance only 30ms (well before 150ms debounce)
        advanceTimeBy(30)
        var state = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(SaveStatus.Saving, state.saveStatus)

        // Switch notes before debounce fires
        viewModel.selectNote("cli-cheatsheet")
        advanceUntilIdle()

        // Verify the previous note was flushed and saved to repository
        val notesInRepo = (viewModel.uiState.value as NoteUiState.Ready).notes
        val previousNoteInRepo = notesInRepo.find { it.id == initialNoteId }
        assertNotNull(previousNoteInRepo)
        assertEquals(unsavedContent, previousNoteInRepo?.content)
    }

    @Test
    fun initialState_loadsThemeAndFontsFromPreferences() = runTest {
        val customPrefs = InMemoryUserPreferencesRepository(
            appTheme = io.salmonllama.MarkPad.model.AppTheme.PAPERGLOW,
            editorFont = io.salmonllama.MarkPad.model.AppFont.SERIF,
            uiFont = io.salmonllama.MarkPad.model.AppFont.MONOSPACE
        )
        val vm = NoteViewModel(
            repository = repository,
            preferencesRepository = customPrefs,
            ioDispatcher = testDispatcher
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }
        advanceUntilIdle()

        val state = vm.uiState.value as NoteUiState.Ready
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.PAPERGLOW, state.appTheme)
        assertEquals(io.salmonllama.MarkPad.model.AppFont.SERIF, state.editorFont)
        assertEquals(io.salmonllama.MarkPad.model.AppFont.MONOSPACE, state.uiFont)
        assertFalse(state.isSettingsDialogOpen)
    }

    @Test
    fun openAndDismissSettingsDialog_togglesState() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertFalse("Settings dialog should start closed", ready.isSettingsDialogOpen)

        viewModel.openSettingsDialog()
        advanceUntilIdle()

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertTrue("Settings dialog should be open", ready.isSettingsDialogOpen)

        viewModel.dismissSettingsDialog()
        advanceUntilIdle()

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertFalse("Settings dialog should be closed", ready.isSettingsDialogOpen)
    }

    @Test
    fun setAppTheme_updatesStateAndPersists() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.setAppTheme(io.salmonllama.MarkPad.model.AppTheme.OLED)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.OLED, ready.appTheme)
        assertEquals(io.salmonllama.MarkPad.model.AppTheme.OLED, preferencesRepository.getAppTheme())
    }

    @Test
    fun setEditorFont_updatesStateAndPersists() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.setEditorFont(io.salmonllama.MarkPad.model.AppFont.CURSIVE)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(io.salmonllama.MarkPad.model.AppFont.CURSIVE, ready.editorFont)
        assertEquals(io.salmonllama.MarkPad.model.AppFont.CURSIVE, preferencesRepository.getEditorFont())
    }

    @Test
    fun setUiFont_updatesStateAndPersists() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.setUiFont(io.salmonllama.MarkPad.model.AppFont.SERIF)
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(io.salmonllama.MarkPad.model.AppFont.SERIF, ready.uiFont)
        assertEquals(io.salmonllama.MarkPad.model.AppFont.SERIF, preferencesRepository.getUiFont())
    }

    @Test
    fun deleteNote_removesTargetNoteFromRepository() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val readyBefore = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(3, readyBefore.notes.size)
        assertTrue(readyBefore.notes.any { it.id == "cli-cheatsheet" })

        viewModel.deleteNote("cli-cheatsheet")
        advanceUntilIdle()

        val readyAfter = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(2, readyAfter.notes.size)
        assertFalse(readyAfter.notes.any { it.id == "cli-cheatsheet" })
    }

    @Test
    fun deleteNote_whenCurrentNoteDeleted_selectsNextNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val readyBefore = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("meeting-retro", readyBefore.selectedNoteId)

        viewModel.deleteNote("meeting-retro")
        advanceUntilIdle()

        val readyAfter = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(2, readyAfter.notes.size)
        assertNotNull(readyAfter.selectedNoteId)
        assertFalse("Deleted note should not be selected", readyAfter.selectedNoteId == "meeting-retro")
        assertEquals(readyAfter.notes.first().id, readyAfter.selectedNoteId)
        assertEquals(readyAfter.selectedNote?.content, readyAfter.previewMarkdown)
    }

    @Test
    fun deleteNote_whenLastRemainingNoteDeleted_clearsSelection() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.deleteNote("meeting-retro")
        advanceUntilIdle()
        viewModel.deleteNote("feature-checklist")
        advanceUntilIdle()
        viewModel.deleteNote("cli-cheatsheet")
        advanceUntilIdle()

        val readyAfter = viewModel.uiState.value as NoteUiState.Ready
        assertTrue(readyAfter.notes.isEmpty())
        assertNull(readyAfter.selectedNoteId)
        assertNull(readyAfter.selectedNote)
        assertEquals("", readyAfter.previewMarkdown)
    }

    @Test
    fun deleteNote_whenUnselectedNoteDeleted_preservesActiveNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val readyBefore = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("meeting-retro", readyBefore.selectedNoteId)

        viewModel.deleteNote("feature-checklist")
        advanceUntilIdle()

        val readyAfter = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("meeting-retro", readyAfter.selectedNoteId)
        assertEquals(2, readyAfter.notes.size)
    }

    @Test
    fun deleteNote_discardsDraftOfDeletedNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.onContentChanged("# In-Flight Edit\n\nTyping unpersisted note")
        viewModel.deleteNote("meeting-retro")
        advanceUntilIdle()

        val readyAfter = viewModel.uiState.value as NoteUiState.Ready
        assertFalse(readyAfter.notes.any { it.id == "meeting-retro" })
    }

    @Test
    fun deleteNote_triggersStatusNotification() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.deleteNote("cli-cheatsheet")
        advanceTimeBy(100)

        val ready = viewModel.uiState.value as NoteUiState.Ready
        assertNotNull(ready.statusMessage)
        assertTrue(ready.statusMessage?.contains("Deleted") == true)
        assertTrue(ready.statusMessage?.contains("Cheat Sheet") == true)
    }

    @Test
    fun initialState_computesTaskCountsFromSelectedNote() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NoteUiState.Ready
        // "meeting-retro" has 3 unchecked action items
        assertEquals(3, ready.totalTaskCount)
        assertEquals(0, ready.completedTaskCount)
    }

    @Test
    fun toggleTaskItem_flipsUncheckedToCheckedAndBack() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initialReady = viewModel.uiState.value as NoteUiState.Ready
        val lines = initialReady.selectedNote!!.content.lines()
        val firstTaskLineIndex = lines.indexOfFirst { it.trim().startsWith("- [ ]") }
        assertTrue("Expected a task line in seed note", firstTaskLineIndex >= 0)

        // Toggle unchecked -> checked
        viewModel.toggleTaskItem(firstTaskLineIndex)
        testScheduler.runCurrent()
        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(3, ready.totalTaskCount)
        assertEquals(1, ready.completedTaskCount)
        assertTrue(ready.selectedNote!!.content.lines()[firstTaskLineIndex].contains("- [x]"))
        assertTrue(ready.previewMarkdown.lines()[firstTaskLineIndex].contains("- [x]"))

        // Advance past debounce to verify repository persistence
        advanceTimeBy(200)
        advanceUntilIdle()
        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(SaveStatus.Saved, ready.saveStatus)

        // Toggle checked -> unchecked
        viewModel.toggleTaskItem(firstTaskLineIndex)
        testScheduler.runCurrent()
        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(3, ready.totalTaskCount)
        assertEquals(0, ready.completedTaskCount)
        assertTrue(ready.selectedNote!!.content.lines()[firstTaskLineIndex].contains("- [ ]"))
    }

    @Test
    fun toggleTaskItem_supportsPlusMarkerNumberedAndNestedTasksAndMultiHashTitle() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val gfmChecklist = """
            ### Sprint Subtasks
            + [ ] Plus task item
            1. [ ] Numbered task item
              - [ ] Nested task item
        """.trimIndent()

        viewModel.onContentChanged(gfmChecklist)
        advanceUntilIdle()

        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("Sprint Subtasks", ready.selectedNote?.title)
        assertEquals(3, ready.totalTaskCount)
        assertEquals(0, ready.completedTaskCount)

        viewModel.toggleTaskItem(1)
        viewModel.toggleTaskItem(2)
        viewModel.toggleTaskItem(3)
        testScheduler.runCurrent()

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(3, ready.completedTaskCount)
        val updatedLines = ready.selectedNote!!.content.lines()
        assertEquals("+ [x] Plus task item", updatedLines[1])
        assertEquals("1. [x] Numbered task item", updatedLines[2])
        assertEquals("  - [x] Nested task item", updatedLines[3])
    }

    @Test
    fun togglePinNote_pinsNoteAndMovesToTopOfList() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Initially "feature-checklist" is last in SeedNotes
        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals("feature-checklist", ready.notes.last().id)
        assertEquals(0, ready.pinnedNotesCount)
        assertTrue(ready.canPinMore)

        viewModel.togglePinNote("feature-checklist")
        advanceTimeBy(100)

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(1, ready.pinnedNotesCount)
        assertTrue(ready.canPinMore)
        assertEquals("feature-checklist", ready.notes.first().id)
        assertTrue(ready.notes.first().isPinned)
        assertNotNull(ready.notes.first().pinnedAt)
        assertTrue(ready.statusMessage?.contains("Pinned") == true)
    }

    @Test
    fun togglePinNote_ordersNewestPinFirst_andUnpinsBackToUpdatedAtOrder() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Pin feature-checklist first, then cli-cheatsheet second
        viewModel.togglePinNote("feature-checklist")
        advanceTimeBy(50)
        Thread.sleep(5) // ensure distinct System.currentTimeMillis()
        viewModel.togglePinNote("cli-cheatsheet")
        advanceTimeBy(50)

        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(2, ready.pinnedNotesCount)
        // Newest pin ("cli-cheatsheet") should be first, older pin ("feature-checklist") second
        assertEquals("cli-cheatsheet", ready.notes[0].id)
        assertEquals("feature-checklist", ready.notes[1].id)
        assertEquals("meeting-retro", ready.notes[2].id)

        // Unpin "cli-cheatsheet" -> should return below pinned "feature-checklist" and below newer "meeting-retro"
        viewModel.togglePinNote("cli-cheatsheet")
        advanceTimeBy(50)

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(1, ready.pinnedNotesCount)
        assertEquals("feature-checklist", ready.notes[0].id)
        assertEquals("meeting-retro", ready.notes[1].id)
        assertEquals("cli-cheatsheet", ready.notes[2].id)
        assertFalse(ready.notes[2].isPinned)
        assertNull(ready.notes[2].pinnedAt)
    }

    @Test
    fun togglePinNote_enforcesMaxThreePinsLimit() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Create a 4th note so we have 4 total notes
        viewModel.createNewNote()
        advanceUntilIdle()

        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(4, ready.notes.size)
        val fourthNoteId = ready.selectedNoteId!!

        // Pin all 3 seed notes
        viewModel.togglePinNote("feature-checklist")
        advanceTimeBy(20)
        viewModel.togglePinNote("cli-cheatsheet")
        advanceTimeBy(20)
        viewModel.togglePinNote("meeting-retro")
        advanceTimeBy(20)

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(3, ready.pinnedNotesCount)
        assertFalse("Should not allow pinning more than 3 notes", ready.canPinMore)

        // Attempt to pin the 4th note
        viewModel.togglePinNote(fourthNoteId)
        advanceTimeBy(50)

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(3, ready.pinnedNotesCount)
        val fourthNote = ready.notes.find { it.id == fourthNoteId }!!
        assertFalse("4th note must remain unpinned", fourthNote.isPinned)
        assertTrue(
            "Should notify about 3-pin limit",
            ready.statusMessage?.contains("Maximum of 3 pinned notes") == true
        )
    }

    @Test
    fun searchNotes_filtersBothPinnedAndUnpinnedNotesPreservingPinOrder() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Pin "feature-checklist"
        viewModel.togglePinNote("feature-checklist")
        advanceTimeBy(50)

        // Search for "Markdown" which appears in both "feature-checklist" (pinned) and "meeting-retro" / "cli-cheatsheet" (unpinned)
        viewModel.updateSearchQuery("Markdown")
        advanceUntilIdle()

        var ready = viewModel.uiState.value as NoteUiState.Ready
        assertTrue(ready.filteredNotes.size >= 2)
        assertEquals("feature-checklist", ready.filteredNotes.first().id)
        assertTrue(ready.filteredNotes.first().isPinned)

        // Search for "Facilitator" which only appears in unpinned "meeting-retro"
        viewModel.updateSearchQuery("Facilitator")
        advanceUntilIdle()

        ready = viewModel.uiState.value as NoteUiState.Ready
        assertEquals(1, ready.filteredNotes.size)
        assertEquals("meeting-retro", ready.filteredNotes.first().id)
        assertFalse(ready.filteredNotes.first().isPinned)
    }
}

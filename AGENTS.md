# AGENTS.md — MarkPad Repository & Architecture Guide

This document provides AI coding agents and LLM contributors with the architectural context, directory structure, build/test workflows, and engineering invariants required to safely and effectively modify **MarkPad** (`io.salmonllama.MarkPad`).

---

## 1. Project Overview & Tech Stack

**MarkPad** is a desktop- and large-screen-optimized Markdown scratchpad and note manager for Android. It features a 3-pane horizontal workspace (Sidebar, Editor, Collapsible Live Preview), live-linked external file editing (Local storage, Linux VM environment, Google Drive), interactive task checklists, customizable themes/typography, and native export to `.pdf`, `.docx`, and `.md`.

- **Application ID / Namespace**: `io.salmonllama.MarkPad` (`.debug` suffix on debug builds)
- **Current Version**: `1.4.1` (`versionCode = 11`)
- **Language & Toolchain**: Kotlin 2.0 (`jvmTarget = "17"`), Java 17, Android Gradle Plugin (AGP), KSP (`com.google.devtools.ksp`)
- **Android SDK Levels**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (Android 8.0+)
- **UI Framework**: Jetpack Compose (`androidx.compose:compose-bom:2024.09.00`) + Material 3 + Material Icons Extended
- **Persistence**:
  - **Jetpack Room (`2.6.1`)**: SQLite database (`markpad_database.db`, schema v3) storing notes, external file link metadata, and pinned note state (`isPinned`, `pinnedAt`).
  - **SharedPreferences (`markpad_preferences`)**: Stores last opened note ID, per-note cursor offsets, active `AppTheme`, editor `AppFont`, and UI `AppFont`.
- **Zero Heavy External Parser/Export Dependencies**:
  - Custom lightweight Markdown block & inline AST parsers (`MarkdownParser`, `InlineMarkdownParser`).
  - Native OOXML `.docx` generation built directly via `java.util.zip.ZipOutputStream` (`DocxExporter`).
  - Native `.pdf` rendering built with `android.graphics.pdf.PdfDocument`, `Canvas`, and `StaticLayout` (`PdfExporter`).

---

## 2. Build, Test & Development Commands

### Environment & Cache Redirection
When building or running Gradle tasks inside an Android Terminal Linux VM where the workspace resides on a shared FUSE mount (`/storage/emulated/...`), **always redirect Gradle caches and project cache directories** to the VM's native `ext4` filesystem (`$HOME/.cache/...`) to avoid FUSE metadata bottlenecks and file locking errors:

```bash
# Run all JVM unit tests (fast, no device/emulator required)
./gradlew testDebugUnitTest --project-cache-dir="$HOME/.cache/projects/markpad/gradle"

# Run a single unit test class
./gradlew testDebugUnitTest --tests "io.salmonllama.MarkPad.viewmodel.NoteViewModelTest" --project-cache-dir="$HOME/.cache/projects/markpad/gradle"

# Assemble the Debug APK
./gradlew assembleDebug --project-cache-dir="$HOME/.cache/projects/markpad/gradle"

# Assemble the Release APK (uses release keystore if env vars are set, otherwise falls back to debug signing)
./gradlew assembleRelease --project-cache-dir="$HOME/.cache/projects/markpad/gradle"
```

> **Note on Execution Environment**: All tests under `app/src/test/` are pure JVM unit tests that execute on the host JVM without requiring `adb` or a connected Android device. Always run `testDebugUnitTest` to verify changes before completing a task.

---

## 3. Repository & Package Structure

```text
app/src/
├── main/
│   ├── AndroidManifest.xml                  # Registers MainActivity + ACTION_VIEW / ACTION_EDIT intent filters for .md/.txt
│   └── java/io/salmonllama/MarkPad/
│       ├── MainActivity.kt                  # Entry point; wires Room, SharedPreferences, AndroidExternalFileGateway, and intents
│       ├── model/
│       │   ├── Note.kt                      # Core domain model (id, title, content, updatedAt, sourceUri, fileSource, sourceFileName, isPinned, pinnedAt)
│       │   ├── FileSource.kt                # Enum: INTERNAL, LOCAL_COMPUTER, LINUX_ENVIRONMENT, GOOGLE_DRIVE + URI authority detection
│       │   ├── AppTheme.kt                  # Enum: LIGHT, DARK, PAPERGLOW, OLED
│       │   └── AppFont.kt                   # Enum: SANS_SERIF, SERIF, MONOSPACE, CURSIVE
│       ├── data/
│       │   ├── NoteRepository.kt            # NoteRepository interface + InMemoryNoteRepository
│       │   ├── RoomNoteRepository.kt        # Room-backed NoteRepository implementation
│       │   ├── UserPreferencesRepository.kt # UserPreferencesRepository interface + InMemory & SharedPreferences implementations
│       │   ├── ExternalFileGateway.kt       # ExternalFileGateway interface + InMemory & Android SAF (ContentResolver) implementations
│       │   ├── SeedNotes.kt                 # Default starter notes populated on initial database creation
│       │   └── local/
│       │       ├── NoteEntity.kt            # Room @Entity("notes") with domain mapping (toDomain / fromDomain)
│       │       ├── NoteDao.kt               # Room @Dao (getAllNotes Flow ordered by isPinned DESC, pinnedAt DESC, updatedAt DESC)
│       │       └── MarkPadDatabase.kt       # RoomDatabase (v3), MIGRATION_1_2, MIGRATION_2_3, and onCreate seed callback
│       ├── viewmodel/
│       │   ├── NoteUiState.kt               # Sealed interface (Loading, Ready) + SaveStatus enum (Saved, Saving, UnsavedChanges)
│       │   ├── NoteViewModel.kt             # Central state container, dual-debounce pipeline, note pinning, task toggling, file sync, export
│       │   └── NoteViewModelFactory.kt      # ViewModelProvider.Factory for MainActivity
│       ├── ui/
│       │   ├── MarkPadScreen.kt             # Root 3-pane Compose layout + TopAppBar + modal dialogs
│       │   ├── components/
│       │   │   ├── SidebarPane.kt           # Left pane: search bar, pinned/unpinned note list, source badges, right-click/long-press pin/delete/unlink menu
│       │   │   ├── EditorPane.kt            # Center pane: BasicTextField, cursor scroll restoration, echo suppression, Shift+Delete hotkey
│       │   │   ├── PreviewPane.kt           # Right pane: scrollable surface wrapping MarkdownRenderer
│       │   │   ├── StatusFooter.kt          # Bottom bar: note/word/char/task counts, linked file indicator, save status
│       │   │   ├── OpenFileDialog.kt        # SAF document picker launcher scoped by FileSource initial URI
│       │   │   ├── ExportNoteDialog.kt      # Format selector (.pdf, .docx, .md) + CreateDocument SAF launcher
│       │   │   └── SettingsDialog.kt        # Theme, Editor Font, and UI Chrome Font selector dialog
│       │   ├── markdown/
│       │   │   ├── MarkdownParser.kt        # Block-level GFM AST parser (Header 1..6, Paragraph, BulletItem, OrderedItem, TaskItem, CodeBlock, Blockquote, AlertBlock, DetailsBlock, FootnoteDefinition, HorizontalRule, Table, Reference links)
│       │   │   ├── InlineMarkdownParser.kt  # Inline GFM span parser (Normal, Bold, Italic, BoldItalic, Strikethrough, InlineCode, Highlight, Underline, KeyboardKey, Superscript, Subscript, Link, Image, FootnoteReference, Escapes, Emojis)
│       │   │   └── MarkdownRenderer.kt      # Compose UI renderer for MarkdownNode & InlineSpan AST + interactive task checkbox & details callbacks
│       │   ├── shortcuts/
│       │   │   └── DesktopShortcuts.kt      # Modifier.handleMarkPadShortcuts (Ctrl+N, Ctrl+O, Ctrl+S, Ctrl+P, Ctrl+E, Ctrl+,)
│       │   └── theme/
│       │       ├── Color.kt                 # Color palettes for Light, Dark, Paperglow, and OLED themes
│       │       ├── Theme.kt                 # MarkPadTheme composable applying color schemes and dynamic UI typography
│       │       └── Type.kt                  # Typography builder parameterized by AppFont
│       └── export/
│           ├── ExportFormat.kt              # Enum: PDF, DOCX, MARKDOWN (extensions, MIME types, display names)
│           ├── NoteExporter.kt              # Common exporter interface: export(note: Note, outputStream: OutputStream)
│           ├── ExportManager.kt             # Dispatches export by format + sanitizeFileName() utility
│           ├── PdfExporter.kt               # Native Android PdfDocument vector exporter + buildRenderOperationsSummary JVM test helper
│           ├── DocxExporter.kt              # Zero-dependency OOXML (.docx) ZIP/XML exporter with external hyperlink relationships
│           └── MarkdownExporter.kt          # UTF-8 plain Markdown exporter
└── test/
    └── java/io/salmonllama/MarkPad/         # Comprehensive JVM unit tests mirroring main packages
```

---

## 4. Core Architectural Invariants & Patterns

When modifying or extending MarkPad, preserve the following architectural invariants:

### 4.1 Interface + In-Memory Fake Pattern (Testability Without Android Mocks)
Every external or platform boundary has a pure Kotlin interface paired with an `InMemory*` implementation in `app/src/main/java/io/salmonllama/MarkPad/data/`:
- `NoteRepository` → `InMemoryNoteRepository` & `RoomNoteRepository`
- `UserPreferencesRepository` → `InMemoryUserPreferencesRepository` & `SharedPreferencesUserPreferencesRepository`
- `ExternalFileGateway` → `InMemoryExternalFileGateway` & `AndroidExternalFileGateway`

**Rule**: Whenever you add or change a method on any of these interfaces, you **must** update both the production Android implementation and the `InMemory*` implementation so `NoteViewModel` default parameters and JVM unit tests continue to work without mocking frameworks.

### 4.2 Dual-Debounce Reactive Pipeline & Editor Echo Suppression
`NoteViewModel` decouples typing responsiveness from expensive parsing and disk/URI I/O using three state layers:
1. **Immediate Draft (`_activeDraft: MutableStateFlow<ActiveNoteDraft?>`)**: Updated synchronously on every keystroke (`onContentChanged`). Drives `selectedNote`, `wordCount`, `charCount`, `completedTaskCount`, and `totalTaskCount` in `NoteUiState.Ready` with zero latency.
2. **Preview Debounce (`_previewDebounceFlow`, `150ms`)**: Updates `previewMarkdown` after a 150ms pause in typing (or immediately upon note selection, task checkbox toggle, or manual save).
3. **Persistence Debounce (`_saveDebounceFlow`, `500ms`)**: Saves the updated note to `NoteRepository` and, if `note.isExternalFile == true`, writes the content back to `ExternalFileGateway.writeFile(sourceUri, content)`.

**Editor Echo Suppression (`EditorPane.kt`)**:
- `EditorPane` maintains a local `textFieldValue` and a set of `emittedContents` per `noteId`.
- When `LaunchedEffect(note?.content)` fires as Room persistence catches up, any incoming text that matches `textFieldValue.text` or is present in `emittedContents` is ignored so asynchronous database emissions never overwrite in-flight typing or reset the cursor position.
- Legitimate external changes (such as clicking a task checkbox in `PreviewPane` or reloading an external file) are not in `emittedContents` and smoothly update `textFieldValue`.
- **Rule**: Do not remove `emittedContents` tracking in `EditorPane.kt`.

### 4.3 Note Title Derivation, Pinning & External File Lifecycle
- **Title Derivation (`NoteViewModel.deriveTitle`)**:
  - Derived automatically from the first non-blank line of `content`, stripping leading `#` characters (`trimStart('#')`) and trimming to 60 characters (`...` suffix if longer).
  - If `content` is blank, it falls back to `fallbackTitle`: for external files, `sourceFileName` without extension; for internal notes, `"Untitled Note"` (or `"Untitled Scratchpad"` on initial creation).
- **Sidebar Note Pinning (`Note.MAX_PINNED_NOTES = 3`)**:
  - Up to 3 notes can be pinned to the top of the sidebar via right-click or long-press on a note card (`NoteViewModel.togglePinNote(id)`).
  - Pinned notes are ordered with the newest pin first (`isPinned DESC, pinnedAt DESC`), visually separated from unpinned notes (`updatedAt DESC`) by a horizontal divider in `SidebarPane`, and display a pin icon on the card.
  - When 3 notes are already pinned (`canPinMore == false`), the "Pin to Top" context menu item on unpinned notes is disabled and displays a tooltip informing the user of the 3-pin limit.
- **External File Linking**:
  - Notes opened via `openExternalFile(uriString, preferredSource)` check if a note with the same `sourceUri` already exists in the repository; if so, it updates and selects that note instead of creating a duplicate.
  - External files have `fileSource != FileSource.INTERNAL` and `sourceUri != null` (`note.isExternalFile == true`).
  - **Unlinking vs. Deleting**: Calling `deleteNote(id)` on an external file removes the note record from MarkPad's Room database ("Close Linked File" in the UI) and never deletes the user's underlying file on disk or Google Drive.

### 4.4 Markdown AST & Exporter Parity (Full GFM Support)
Markdown parsing is split into two stages:
1. **Block Parsing (`MarkdownParser.parse(markdown: String): List<MarkdownNode>`)**:
   - Performs a reference-link pre-pass (`[label]: url "title"`) outside fenced code blocks and resolves `[text][label]`, `[label][]`, and `[label]` references across all blocks.
   - Produces `Header(level 1..6)` (ATX `#`–`######` and Setext `===`/`---`), `Paragraph`, `BulletItem(text, indentLevel)` (`-`, `*`, `+`), `OrderedItem(number, text, indentLevel)` (`1.`, `1)`), `TaskItem(isChecked, text, lineIndex, indentLevel)`, `CodeBlock(language, code)` (backtick/tilde fences and 4-space indented blocks), `Blockquote(text, depth)`, `AlertBlock(type: AlertType, text)` (`[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, `[!CAUTION]`), `DetailsBlock(summary, body, isOpenByDefault)`, `FootnoteDefinition(id, text)`, `HorizontalRule`, and `Table(headers, alignments, rows)`.
2. **Inline Parsing (`InlineMarkdownParser.parse(input: String): List<InlineSpan>`)**:
   - Left-to-right GFM scanner producing `Normal` (with backslash escapes and `:emoji:` shortcodes resolved), `Bold` (`**`, `__`), `Italic` (`*`, `_` with GFM intraword underscore preservation), `BoldItalic` (`***`, `___`, `**_`, `__*`), `Strikethrough` (`~~`, `~`, `<del>`, `<s>`), `InlineCode` (single/multi-backtick `` ` `` / ```` `` ````), `Highlight` (`==`, `<mark>`), `Underline` (`<ins>`, `<u>`), `KeyboardKey` (`<kbd>`), `Superscript` (`<sup>`), `Subscript` (`<sub>`), `Link` (`[text](url "title")` and GFM URL/email autolinks), `Image` (`![alt](url "title")`), and `FootnoteReference` (`[^id]`).

**Rule**: If you add a new `MarkdownNode` subtype or `InlineSpan` subtype, you **must** handle it across all 3 rendering/export targets and their tests:
1. `ui/markdown/MarkdownRenderer.kt` (Compose live preview)
2. `export/PdfExporter.kt` (Android PDF canvas renderer + `buildRenderOperationsSummary`)
3. `export/DocxExporter.kt` (OOXML `word/document.xml`, `word/styles.xml`, and `word/_rels/document.xml.rels` builder)
4. Unit tests in `app/src/test/java/io/salmonllama/MarkPad/markdown/` and `export/`

### 4.5 Interactive Task Toggling
- `MarkdownNode.TaskItem` records the exact zero-based `lineIndex` from `markdown.lines()` as well as `indentLevel`.
- When a user clicks a task checkbox in `PreviewPane`, `NoteViewModel.toggleTaskItem(lineIndex)` uses `^(\s*(?:[-*+]|\d+[.)])\s+\[)([ xX])(].*)$` to flip `[ ]` ↔ `[x]` on that specific line (supporting `-`, `*`, `+`, numbered, and nested tasks), immediately updates `_activeDraft` and `_previewMarkdown`, and schedules persistence.

### 4.6 Room Database Schema & Migrations
- `MarkPadDatabase` is currently at **`version = 3`**.
- Version 1 → 2 migration (`MIGRATION_1_2`) added `sourceUri`, `fileSource`, and `sourceFileName` columns to the `notes` table.
- Version 2 → 3 migration (`MIGRATION_2_3`) added `isPinned` (`INTEGER NOT NULL DEFAULT 0`) and `pinnedAt` (`INTEGER`) columns to the `notes` table.
- **Rule**: Never use `fallbackToDestructiveMigration()`. If you modify `NoteEntity.kt`, increment the `@Database(version = ...)` number in `MarkPadDatabase.kt`, add a corresponding `Migration(oldVersion, newVersion)` object, register it in `.addMigrations(...)`, and add mapping tests in `NoteEntityTest.kt`.

### 4.7 Theme Contrast & Accessibility Invariant
- MarkPad supports 4 themes (`AppTheme.LIGHT`, `DARK`, `PAPERGLOW`, `OLED`) defined in `ui/theme/Color.kt` and `ui/theme/Theme.kt`.
- `ThemeAccessibilityTest.kt` enforces WCAG contrast ratios across all theme surface/onSurface and primary/onPrimary pairs.
- **Rule**: Any edits to theme colors in `Color.kt` or `Theme.kt` must pass `./gradlew testDebugUnitTest --tests "io.salmonllama.MarkPad.ui.theme.ThemeAccessibilityTest"`.

### 4.8 Keyboard Shortcuts & Desktop Ergonomics
- Global shortcuts are handled in `ui/shortcuts/DesktopShortcuts.kt` via `onPreviewKeyEvent`:
  - `Ctrl + N`: New scratchpad
  - `Ctrl + O`: Open external Markdown file dialog
  - `Ctrl + S`: Immediate save (`triggerSave`)
  - `Ctrl + P`: Toggle right-hand preview pane
  - `Ctrl + E`: Open export dialog
  - `Ctrl + ,`: Open aesthetic & typography settings dialog
- Editor-specific shortcuts in `ui/components/EditorPane.kt`:
  - `Shift + Delete`: Deletes the entire line at the current cursor position (`deleteLineAtCursor`), preserving column offset. Tested in `EditorLineDeleteTest.kt`.

---

## 5. Testing Conventions for Agents

All unit tests live in `app/src/test/java/io/salmonllama/MarkPad/` and run on the host JVM:

- **ViewModel & Coroutine Timing Tests (`NoteViewModelTest.kt`, `ExternalFileOpenTest.kt`)**:
  - Inject `StandardTestDispatcher(testScheduler)` as `ioDispatcher` (or set `Dispatchers.setMain`) inside `runTest`.
  - Remember the debounce thresholds when testing reactive flows:
    - Advance time by **`> 150ms`** (e.g., `advanceTimeBy(200)`) to assert `previewMarkdown` updates.
    - Advance time by **`> 500ms`** (e.g., `advanceTimeBy(600)`) to assert auto-save persistence and external file write-back.
- **DOCX Exporter Tests (`DocxExporterTest.kt`)**:
  - Export to a `ByteArrayOutputStream`, read back via `ZipInputStream`, and assert that `[Content_Types].xml`, `_rels/.rels`, `word/_rels/document.xml.rels`, `word/styles.xml`, and `word/document.xml` exist and contain expected OOXML tags (`<w:p>`, `<w:tbl>`, `<w:r>`, etc.).
- **PDF Exporter Tests (`PdfExporterTest.kt`)**:
  - Since `android.graphics.pdf.PdfDocument` is stubbed in default Android JVM unit tests, `PdfExporter` accepts an injectable `documentWriter: (Note, OutputStream) -> Unit` constructor parameter for JVM verification.
- **Editor Text Manipulation Tests (`EditorLineDeleteTest.kt`)**:
  - Keep non-trivial text transformation helpers as `internal` pure functions operating on `TextFieldValue` (like `deleteLineAtCursor`) so edge cases (single line, first line, middle line, last line, cursor clamping) can be exhaustively tested in pure JUnit tests.

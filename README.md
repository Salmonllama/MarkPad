# MarkPad — Markdown Notes & Scratchpad Manager

**MarkPad** is a fast, desktop- and large-screen-optimized Markdown scratchpad and note manager for Android, built with **Jetpack Compose** and **Material 3**. Designed for distraction-free writing, real-time side-by-side previewing, and ergonomic physical-keyboard workflows, MarkPad lets you manage quick scratchpads alongside live-linked external Markdown files from local storage, Linux VM environments, and Google Drive.

---

## ✨ Key Features

- **3-Pane Desktop-Class Workspace**
  - **Sidebar Pane**: Instant full-text search across note titles and content, pinned notes section (up to 3 pinned notes at the top), last-modified timestamps, file source badges, and quick actions to create or open files.
  - **Editor Pane**: Responsive, zero-lag text editor with automatic per-note cursor position memory and debounced background auto-saving.
  - **Live Preview Pane (Collapsible)**: Real-time rendered Markdown preview that updates smoothly as you type and can be toggled on or off anytime.
- **Live-Linked External Files**
  - Open existing `.md` and `.txt` files directly from **Local Computer (`Documents`)**, **Linux Environment (`Documents/source`)**, or **Google Drive**.
  - Opened files remain live-linked to their source URI—saving (`Ctrl + S`) writes your edits directly back to the original file on disk or cloud storage.
- **Multi-Format Export**
  - Export any scratchpad or note to **PDF Document (`.pdf`)**, **Microsoft Word Document (`.docx`)**, or **Markdown (`.md`)** with rich formatting preserved (headers, tables, task lists, code blocks, and blockquotes).
- **Interactive Task Lists**
  - Click `- [ ]` / `- [x]` checkboxes directly inside the **Live Preview** pane to toggle task completion in your Markdown source and track progress in the bottom status bar.
- **Aesthetic & Typography Customization**
  - **4 Curated Themes**:
    - **Dark** — Default slate dark theme
    - **Light** — Clean daylight theme with high contrast
    - **Paperglow** — Warm parchment and sepia tones for comfortable long-form reading
    - **OLED** — Pitch-black background optimized for OLED displays
  - **Independent Font Selection**: Customize your **Editor Font** and **UI Chrome Font** independently across **Sans-Serif (Modern)**, **Serif (Editorial)**, **Monospace (Code)**, and **Cursive (Script)**.
- **Live Status Footer**
  - Displays total scratchpad count, real-time word and character counts, task completion progress (`completed/total tasks`), active external file link badge, and current save status.

---

## 📖 How to Use MarkPad

### 1. Creating & Managing Scratchpads
- **Create a New Note**: Click the **+ New** button at the bottom of the left sidebar or press `Ctrl + N`.
- **Automatic Note Titles**: MarkPad automatically derives a note's title from its **first non-empty line** (stripping leading `#` heading markers). If you open an external file without a top heading, the file's name is used.
- **Auto-Save & Manual Save**: Edits are automatically persisted in the background after a brief pause in typing. You can also click the **Save (✓)** icon in the top bar or press `Ctrl + S` to immediately flush changes.
- **Pinning Notes to the Top**:
  - **Right-click** or **long-press** any note in the sidebar and select **Pin to Top** to pin it to the top of the sidebar list (ordered newest pin first and separated from unpinned notes by a divider).
  - Up to **3 notes** can be pinned at a time to preserve space for your unpinned notes. When 3 notes are pinned, the **Pin to Top** option is disabled with a tooltip reminding you of the 3-pin limit.
  - Right-click or long-press a pinned note and select **Unpin Note** to return it to the regular list.
- **Deleting or Unlinking Notes**:
  - **Right-click** (with a mouse/trackpad) or **long-press** (touch) any note in the sidebar to open its context menu.
  - For internal scratchpads, select **Delete Note → Confirm Delete** to permanently remove the note.
  - For external linked files, select **Close Linked File → Confirm Unlink** to remove it from MarkPad's sidebar while leaving the original file on your device or cloud drive intact.

### 2. Searching Your Notes
- Use the **Search scratchpads...** field at the top of the left sidebar to filter both your pinned and unpinned notes in real time by title, body content, or linked filename.

### 3. Opening & Editing External Markdown Files
1. Click the **Open** button at the bottom of the sidebar, click the **Folder** icon in the top app bar, or press `Ctrl + O`.
2. In the **Open Markdown File** dialog, choose where your file is located:
   - **Local Computer** — Local device storage (`Documents`)
   - **Linux Environment** — Shared Linux workspace (`Documents/source`)
   - **Google Drive** — Cloud storage via the system document provider
3. Click **Browse** and select your `.md` or `.txt` file.
4. A source badge (**Local**, **Linux**, or **Drive**) will appear on the note card in the sidebar and in the bottom status bar. All edits made in MarkPad will sync back to that file when saved.

### 4. Using the Live Preview & Interactive Checklists
- **Toggle Preview**: Click the **Eye** icon in the top app bar or press `Ctrl + P` to show or hide the right-hand preview pane when you want a wider editor view.
- **Check Off Tasks**: Write task items using `- [ ] Task description` or `- [x] Completed task`. Click any checkbox in the **Preview Pane** to toggle its state immediately, and watch your overall task progress update in the bottom status footer.

### 5. Exporting Notes
1. Select the note you want to export.
2. Click the **Export (Download)** icon in the top app bar or press `Ctrl + E`.
3. Choose your target format:
   - **PDF Document (`.pdf`)** — High-fidelity vector document ready for sharing or printing
   - **Word Document (`.docx`)** — Native OOXML document compatible with Microsoft Word and Google Docs
   - **Markdown (`.md`)** — Plain-text Markdown file
4. Click **Choose Destination** to pick a save location and filename.

### 6. Customizing Themes & Fonts
1. Click the **Settings (Gear)** icon in the top app bar or press `Ctrl + ,`.
2. Select your preferred **Color Theme** (*Light*, *Dark*, *Paperglow*, or *OLED*).
3. Choose your **Editor Font** (used while writing Markdown) and **UI Chrome Font** (used for sidebars, dialogs, and headers). Preferences are saved automatically.

---

## ⌨️ Keyboard Shortcuts

MarkPad is built with first-class physical keyboard support for laptops, tablets with keyboards, and desktop environments:

| Shortcut | Action |
| :--- | :--- |
| `Ctrl + N` | Create a new scratchpad |
| `Ctrl + O` | Open an external Markdown file |
| `Ctrl + S` | Force save / persist changes immediately |
| `Ctrl + P` | Toggle the right-hand Markdown preview pane |
| `Ctrl + E` | Open the Export Note dialog (`.pdf`, `.docx`, `.md`) |
| `Ctrl + ,` | Open Aesthetic & Typography Settings |
| `Shift + Delete` | Delete the entire line at the current cursor position in the editor |

---

## 📝 Supported GitHub Flavored Markdown (GFM) Syntax

MarkPad's parser, live preview renderer, and native `.pdf` / `.docx` exporters support full syntactic **GitHub Flavored Markdown (GFM)**:

### Block-Level Syntax

| Element | Syntax Example |
| :--- | :--- |
| **ATX Headers (H1–H6)** | `# H1`, `## H2`, `### H3`, `#### H4`, `##### H5`, `###### H6` (with optional closing `#`s) |
| **Setext Headers (H1–H2)** | `Heading 1` underlined with `===` or `Heading 2` underlined with `---` |
| **Fenced Code Blocks** | ` ```kotlin ` or ` ~~~bash ` followed by code and matching closing fence |
| **Indented Code Blocks** | Lines indented by 4 spaces or 1 tab following a blank line |
| **Unordered Lists & Nesting** | `- Item`, `* Item`, or `+ Item` (indent by 2+ spaces for nested sublists) |
| **Ordered Lists & Nesting** | `1. First`, `2. Second`, or `1) Item` (supports nested numbering) |
| **Interactive Task Lists** | `- [ ] Pending`, `- [x] Done`, `+ [X] Done`, `1. [ ] Numbered task` (with nesting) |
| **Blockquotes & Nesting** | `> Quoted text` and nested `>> Second-level quote` |
| **GitHub Alerts / Callouts** | `> [!NOTE]`, `> [!TIP]`, `> [!IMPORTANT]`, `> [!WARNING]`, `> [!CAUTION]` |
| **Collapsible Details** | `<details><summary>Title</summary>Hidden Markdown content</details>` |
| **Footnotes** | `[^1]` inline reference paired with `[^1]: Footnote definition` |
| **Reference Link Definitions** | `[label]: https://example.com "Optional Title"` |
| **Thematic Breaks (HR)** | `---`, `***`, `___`, `- - -`, `* * *`, `_ _ _` |
| **GFM Tables** | Pipe-delimited tables with `:---`, `:---:`, `---:` alignment and escaped pipes (`\|`) |

### Inline Syntax

| Element | Syntax Example |
| :--- | :--- |
| **Bold** | `**bold text**` or `__bold text__` |
| **Italic** | `*italic text*` or `_italic text_` (preserves intraword `snake_case_names`) |
| **Bold & Italic** | `***bold italic***`, `___bold italic___`, `**_mixed_**`, `__*mixed*__` |
| **Strikethrough** | `~~strikethrough~~` or `~strikethrough~` |
| **Inline Code** | `` `inline code` `` or ```` ``code with ` backtick`` ```` |
| **Links & Reference Links** | `[GitHub](https://github.com "Title")`, `[Text][ref]`, `[ref]` |
| **Autolinks** | `https://github.com`, `www.example.com`, `<https://...>`, `<user@example.com>` |
| **Images** | `![Alt text](https://example.com/diagram.png "Title")` |
| **Highlight / Mark** | `==highlighted text==` or `<mark>highlighted text</mark>` |
| **Underline / Inserted** | `<ins>inserted text</ins>` or `<u>underlined text</u>` |
| **Keyboard Keys** | `<kbd>Ctrl + S</kbd>` |
| **Superscript & Subscript** | `x<sup>2</sup>` and `H<sub>2</sub>O` |
| **Emoji Shortcodes** | `:rocket:` (🚀), `:warning:` (⚠️), `:white_check_mark:` (✅), `:bulb:` (💡), `:tada:` (🎉) |
| **Backslash Escapes** | `\*literal asterisks\*`, `\_`, `\~`, `` \` ``, `\[`, `\\` |

### Example GFM Alert & Table

```markdown
> [!TIP]
> Press <kbd>Ctrl + E</kbd> to export your note directly to `.pdf` or `.docx` with all GFM styling preserved!

| Capability | Status | Alignment |
| :--- | :---: | ---: |
| GFM Alerts & Details | **Active** | Left |
| DOCX Hyperlinks & Styles | **Native** | Center |
| Vector PDF Rendering | **Direct** | Right |
```

---

## 🛠️ Building from Source

### Prerequisites
- **Android SDK**: `compileSdk 36` (`minSdk 26` / Android 8.0+)
- **JDK**: Java 17

### Build & Run
Clone the repository and build the debug APK using the Gradle wrapper:

```bash
# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

The generated debug APK will be located in `app/build/outputs/apk/debug/`.

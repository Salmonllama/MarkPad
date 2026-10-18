package io.salmonllama.MarkPad.data

import io.salmonllama.MarkPad.model.Note

object SeedNotes {
    val items: List<Note> = listOf(
        Note(
            id = "meeting-retro",
            title = "Sprint 42 Retrospective & Sync",
            content = """
                # Sprint 42 Retrospective & Sync

                **Date:** October 12, 2026
                **Facilitator:** @alex | **Participants:** @jordan, @taylor, @morgan

                ---

                ## What Went Well
                - Multi-pane Material 3 layout migration completed ahead of schedule.
                - Automated UI snapshot tests running smoothly on host JVM.
                - Reduced cold-start latency by `35%`.

                ## Action Items
                - [ ] Implement debounced Markdown parser preview pipeline.
                - [ ] Add global physical key bindings (`Ctrl + N`, `Ctrl + S`, `Ctrl + P`).
                - [ ] Profile memory usage on Googlebook low-power state.

                > *"Premature optimization is the root of all evil, but ergonomics are non-negotiable."*
            """.trimIndent(),
            updatedAt = System.currentTimeMillis() - 3600_000
        ),
        Note(
            id = "cli-cheatsheet",
            title = "Developer CLI & Tooling Cheat Sheet",
            content = """
                # Developer CLI & Tooling Cheat Sheet

                Quick reference for daily developer workflows on Linux and Android VM environments.

                ## Git Diagnostics
                ```bash
                # Inspect recent commits with compact graph
                git log --graph --oneline --decorate -n 15

                # Clean untracked build artifacts
                git clean -fdx -e .idea/
                ```

                ## Gradle & Cache Flags
                Always redirect cache directories to avoid FUSE metadata overhead:
                ```bash
                ./gradlew testDebugUnitTest --project-cache-dir="${'$'}HOME/.cache/projects/markpad/gradle"
                ```

                ## Essential Hotkeys
                - `Ctrl + N`: Instant new scratchpad
                - `Ctrl + S`: Force save / persist
                - `Ctrl + P`: Toggle Markdown preview pane
            """.trimIndent(),
            updatedAt = System.currentTimeMillis() - 7200_000
        ),
        Note(
            id = "feature-checklist",
            title = "MarkPad Architecture Deliverables",
            content = """
                # MarkPad Architecture Deliverables

                Roadmap and verification criteria for desktop-optimized scratchpad manager.

                ## Core Milestones
                - **M1:** Horizontal 3-pane responsive layout (Sidebar, Editor, Preview).
                - **M2:** Monospace typography with 24sp line height for code readability.
                - **M3:** Live debounced syncing (150ms) to ensure zero editor input lag.
                - **M4:** In-memory repository with clean domain separation for future Room integration.

                ## Feature Status Matrix

                | Capability | Status | Alignment | Performance |
                | :--- | :---: | :---: | ---: |
                | Markdown Tables | **Active** | Left | Instant (<16ms) |
                | DOCX Export | **Native** | Center | Zero runtime overhead |
                | PDF Export | **Direct** | Right | High-fidelity vector |
                | Debounced Sync | *Active* | Right | 150ms latency |

                ## Markdown Capabilities
                - Headers `#`, `##`, `###`
                - Bold `**text**` and Italic `*text*`
                - Inline code blocks `` `code` ``
                - Multi-line fenced code blocks with dark surface containers
                - Bullet lists with indented items
                - GitHub Flavored Markdown tables with column alignments
            """.trimIndent(),
            updatedAt = System.currentTimeMillis() - 14400_000
        )
    )
}

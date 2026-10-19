package io.salmonllama.MarkPad.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlin.math.roundToInt

enum class ScrollDriver {
    NONE,
    EDITOR,
    PREVIEW
}

object SynchronizedScrollHelper {
    /**
     * Calculates the normalized scroll fraction between 0.0 and 1.0.
     */
    fun calculateScrollFraction(value: Int, maxValue: Int): Float {
        if (maxValue <= 0) return 0f
        return (value.toFloat() / maxValue).coerceIn(0f, 1f)
    }

    /**
     * Maps a normalized fraction [0.0, 1.0] to a target scroll offset.
     */
    fun calculateTargetScroll(fraction: Float, targetMax: Int): Int {
        if (targetMax <= 0) return 0
        val clampedFraction = fraction.coerceIn(0f, 1f)
        return (clampedFraction * targetMax).roundToInt().coerceIn(0, targetMax)
    }

    /**
     * Converts a scroll offset from a source scroll state to the proportional
     * scroll offset in a target scroll state.
     */
    fun calculateSynchronizedTarget(sourceValue: Int, sourceMax: Int, targetMax: Int): Int {
        if (sourceMax <= 0 || targetMax <= 0) return 0
        val fraction = calculateScrollFraction(sourceValue, sourceMax)
        return calculateTargetScroll(fraction, targetMax)
    }

    /**
     * Verifies if a candidate scroll driver can take control of the synchronization.
     */
    fun canAcquireDriver(current: ScrollDriver, candidate: ScrollDriver): Boolean {
        if (candidate == ScrollDriver.NONE) return true
        return current == ScrollDriver.NONE || current == candidate
    }

    /**
     * Releases the active driver back to NONE if it matches the releasing driver.
     */
    fun releaseDriver(current: ScrollDriver, releasing: ScrollDriver): ScrollDriver {
        return if (current == releasing) ScrollDriver.NONE else current
    }
}

/**
 * Coordinates bidirectional synchronous scrolling between [editorScrollState] and
 * [previewScrollState].
 *
 * Uses a mutual exclusion driver state machine to prevent recursive feedback loops
 * and scroll mutex cancellation in Jetpack Compose.
 */
@Composable
fun SynchronizedScrollCoordinator(
    editorScrollState: ScrollState,
    previewScrollState: ScrollState,
    isEditorVisible: Boolean = true,
    isPreviewVisible: Boolean = true,
    noteId: String? = null
) {
    var activeDriver by remember { mutableStateOf(ScrollDriver.NONE) }
    var initialSyncCompleted by remember(noteId) { mutableStateOf(false) }
    var lastSingleVisiblePane by remember { mutableStateOf(ScrollDriver.NONE) }

    // Track single pane visibility to intelligently re-sync when split view is restored
    LaunchedEffect(isEditorVisible, isPreviewVisible) {
        if (isEditorVisible && !isPreviewVisible) {
            lastSingleVisiblePane = ScrollDriver.EDITOR
        } else if (!isEditorVisible && isPreviewVisible) {
            lastSingleVisiblePane = ScrollDriver.PREVIEW
        } else if (isEditorVisible && isPreviewVisible) {
            val editorMax = editorScrollState.maxValue
            val previewMax = previewScrollState.maxValue
            val editorReady = editorMax in 1 until Int.MAX_VALUE
            val previewReady = previewMax in 1 until Int.MAX_VALUE

            if (editorReady && previewReady) {
                if (lastSingleVisiblePane == ScrollDriver.PREVIEW) {
                    val target = SynchronizedScrollHelper.calculateSynchronizedTarget(
                        sourceValue = previewScrollState.value,
                        sourceMax = previewMax,
                        targetMax = editorMax
                    )
                    if (editorScrollState.value != target) {
                        editorScrollState.scrollTo(target)
                    }
                } else if (lastSingleVisiblePane == ScrollDriver.EDITOR) {
                    val target = SynchronizedScrollHelper.calculateSynchronizedTarget(
                        sourceValue = editorScrollState.value,
                        sourceMax = editorMax,
                        targetMax = previewMax
                    )
                    if (previewScrollState.value != target) {
                        previewScrollState.scrollTo(target)
                    }
                }
            }
            lastSingleVisiblePane = ScrollDriver.NONE
        }
    }

    // Align initial scroll location once both panes have measured their bounds for the current note
    LaunchedEffect(noteId, editorScrollState.maxValue, previewScrollState.maxValue, isEditorVisible, isPreviewVisible) {
        if (!initialSyncCompleted && isEditorVisible && isPreviewVisible) {
            val editorMax = editorScrollState.maxValue
            val previewMax = previewScrollState.maxValue
            val editorReady = editorMax in 1 until Int.MAX_VALUE
            val previewReady = previewMax in 1 until Int.MAX_VALUE

            if (editorReady && previewReady) {
                val targetPreview = SynchronizedScrollHelper.calculateSynchronizedTarget(
                    sourceValue = editorScrollState.value,
                    sourceMax = editorMax,
                    targetMax = previewMax
                )
                if (previewScrollState.value != targetPreview) {
                    previewScrollState.scrollTo(targetPreview)
                }
                initialSyncCompleted = true
            } else if (editorMax == 0 || previewMax == 0) {
                initialSyncCompleted = true
            }
        }
    }

    // Listen to Editor scroll events and sync to Preview
    LaunchedEffect(editorScrollState, previewScrollState, isEditorVisible, isPreviewVisible) {
        if (!isEditorVisible || !isPreviewVisible) return@LaunchedEffect
        snapshotFlow {
            Triple(
                editorScrollState.value,
                editorScrollState.maxValue,
                editorScrollState.isScrollInProgress
            )
        }.collect { (editorValue, editorMax, inProgress) ->
            if (inProgress && activeDriver == ScrollDriver.NONE) {
                activeDriver = ScrollDriver.EDITOR
            }
            if (activeDriver == ScrollDriver.EDITOR) {
                val previewMax = previewScrollState.maxValue
                if (editorMax in 1 until Int.MAX_VALUE && previewMax in 1 until Int.MAX_VALUE) {
                    val target = SynchronizedScrollHelper.calculateSynchronizedTarget(
                        sourceValue = editorValue,
                        sourceMax = editorMax,
                        targetMax = previewMax
                    )
                    if (previewScrollState.value != target) {
                        previewScrollState.scrollTo(target)
                    }
                }
                if (!inProgress) {
                    activeDriver = ScrollDriver.NONE
                }
            }
        }
    }

    // Listen to Preview scroll events and sync to Editor
    LaunchedEffect(editorScrollState, previewScrollState, isEditorVisible, isPreviewVisible) {
        if (!isEditorVisible || !isPreviewVisible) return@LaunchedEffect
        snapshotFlow {
            Triple(
                previewScrollState.value,
                previewScrollState.maxValue,
                previewScrollState.isScrollInProgress
            )
        }.collect { (previewValue, previewMax, inProgress) ->
            if (inProgress && activeDriver == ScrollDriver.NONE) {
                activeDriver = ScrollDriver.PREVIEW
            }
            if (activeDriver == ScrollDriver.PREVIEW) {
                val editorMax = editorScrollState.maxValue
                if (previewMax in 1 until Int.MAX_VALUE && editorMax in 1 until Int.MAX_VALUE) {
                    val target = SynchronizedScrollHelper.calculateSynchronizedTarget(
                        sourceValue = previewValue,
                        sourceMax = previewMax,
                        targetMax = editorMax
                    )
                    if (editorScrollState.value != target) {
                        editorScrollState.scrollTo(target)
                    }
                }
                if (!inProgress) {
                    activeDriver = ScrollDriver.NONE
                }
            }
        }
    }
}

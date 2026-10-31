package ru.aokazantsev.devatlas.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReorderStateTest {

    private val keys = listOf("a", "b", "c")

    private fun stateWithRows(): ReorderState = ReorderState().apply {
        keys.forEachIndexed { index, key -> place(key, ReorderBounds(top = index * ROW, height = ROW)) }
    }

    @Test
    fun pressOutsideRowsDoesNotStartDrag() {
        val state = stateWithRows()

        assertFalse(state.start(keys, 3 * ROW + 1))
        assertNull(state.dragged)
    }

    @Test
    fun draggingDownPassesNeighboursByTheirCenters() {
        val state = stateWithRows()
        assertTrue(state.start(keys, 10f))

        state.dragTo(10f + ROW * 0.4f)
        assertEquals(keys, state.displayed(keys))

        state.dragTo(10f + ROW * 0.6f)
        assertEquals(listOf("b", "a", "c"), state.displayed(keys))

        state.dragTo(10f + ROW * 1.7f)
        assertEquals(listOf("b", "c", "a"), state.displayed(keys))

        assertEquals(0 to 2, state.finish(commit = true))
        assertNull(state.dragged)
    }

    @Test
    fun oneLongMoveJumpsSeveralRowsAndBack() {
        val state = stateWithRows()
        state.start(keys, 10f)

        state.dragTo(10f + ROW * 2)
        assertEquals(listOf("b", "c", "a"), state.displayed(keys))

        state.dragTo(10f)
        assertEquals(keys, state.displayed(keys))
        assertNull(state.finish(commit = true))
    }

    @Test
    fun gapBetweenRowsIsKept() {
        val state = ReorderState().apply {
            keys.forEachIndexed { index, key -> place(key, ReorderBounds(top = index * (ROW + GAP), height = ROW)) }
        }
        state.start(keys, 2 * (ROW + GAP) + 10f)

        state.dragTo(2 * (ROW + GAP) + 10f - ROW * 0.6f - GAP)

        assertEquals(listOf("a", "c", "b"), state.displayed(keys))
    }

    @Test
    fun draggedRowFollowsPointer() {
        val state = stateWithRows()
        state.start(keys, ROW + 5f)

        state.dragTo(ROW + 25f)

        assertEquals(20f, state.translation("b"))
        assertEquals(0f, state.translation("a"))
    }

    @Test
    fun draggingUpMovesRowHigher() {
        val state = stateWithRows()
        state.start(keys, 2 * ROW + 10f)

        state.dragTo(2 * ROW + 10f - ROW * 0.6f)

        assertEquals(listOf("a", "c", "b"), state.displayed(keys))
        assertEquals(2 to 1, state.finish(commit = true))
    }

    @Test
    fun releaseInPlaceReportsNoMove() {
        val state = stateWithRows()
        state.start(keys, 10f)

        state.dragTo(15f)

        assertNull(state.finish(commit = true))
        assertEquals(keys, state.displayed(keys))
    }

    @Test
    fun afterReleaseOwnerOrderIsShown() {
        val state = stateWithRows()
        state.start(keys, 10f)
        state.dragTo(10f + ROW * 0.6f)
        assertEquals(listOf("b", "a", "c"), state.displayed(keys))

        assertEquals(0 to 1, state.finish(commit = true))

        assertEquals(listOf("b", "a", "c"), state.displayed(keys))
        assertEquals(listOf("b", "a", "c"), state.displayed(listOf("b", "a", "c")))
    }

    @Test
    fun settledOrderBridgesTheFrameBeforeOwnerCatchesUp() {
        val state = stateWithRows()
        state.start(keys, 10f)
        state.dragTo(10f + ROW * 0.6f)
        state.finish(commit = true)

        assertTrue(state.isAwaitingOwner(keys))
        assertEquals(listOf("b", "a", "c"), state.displayed(keys))

        assertFalse(state.isAwaitingOwner(listOf("b", "a", "c")))
        state.release()
        assertEquals(keys, state.displayed(keys))
    }

    @Test
    fun settledOrderIsDroppedWhenRowsChange() {
        val state = stateWithRows()
        state.start(keys, 10f)
        state.dragTo(10f + ROW * 0.6f)
        state.finish(commit = true)

        val changed = listOf("a", "c")

        assertFalse(state.isAwaitingOwner(changed))
        assertEquals(changed, state.displayed(changed))
    }

    @Test
    fun cancelledGestureRestoresOriginalOrderWithoutMove() {
        val state = stateWithRows()
        state.start(keys, 10f)
        state.dragTo(10f + ROW * 1.7f)
        assertEquals(listOf("b", "c", "a"), state.displayed(keys))

        assertNull(state.finish(commit = false))

        assertNull(state.dragged)
        assertNull(state.settled)
        assertEquals(keys, state.displayed(keys))
    }

    private companion object {
        const val ROW = 100f
        const val GAP = 4f
    }
}

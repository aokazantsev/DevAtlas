package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrollPositionTest {

    private val scope = SaverScope { true }

    private fun save(position: ScrollPosition): List<Int>? = with(ScrollPosition.Saver) { scope.save(position) }

    @Test
    fun routeAnchorWaitsForRestore() {
        val position = ScrollPosition(7)

        assertEquals(ReadingPlace(7, 0), position.pendingPlace)
        assertEquals(7, position.current())
    }

    @Test
    fun zeroAnchorMeansTopWithoutRestore() {
        assertNull(ScrollPosition(0).pendingPlace)
    }

    @Test
    fun offsetInsideFirstBlockIsRestored() {
        assertEquals(ReadingPlace(0, 120), ScrollPosition(0, 120).pendingPlace)
    }

    @Test
    fun pendingPlaceSurvivesRecreation() {
        val restored = ScrollPosition.Saver.restore(save(ScrollPosition(5, 40))!!)

        assertEquals(ReadingPlace(5, 40), restored?.pendingPlace)
    }

    @Test
    fun restoredPositionWithoutPlacedBlocksSavesTop() {
        val position = ScrollPosition(5)
        position.markRestored()

        assertEquals(listOf(0, 0), save(position))
        assertNull(ScrollPosition.Saver.restore(listOf(0, 0))?.pendingPlace)
    }
}

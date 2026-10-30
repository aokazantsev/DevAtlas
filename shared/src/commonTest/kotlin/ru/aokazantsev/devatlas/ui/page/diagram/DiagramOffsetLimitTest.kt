package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class DiagramOffsetLimitTest {

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x + 0f)
        assertEquals(expected.y, actual.y + 0f)
    }

    private val viewport = IntSize(1000, 2000)
    private val diagram = IntSize(1000, 500)

    @Test
    fun fittedDiagramDoesNotMove() {
        assertOffset(Offset.Zero, Offset(300f, -300f).limitedTo(viewport, diagram, scale = 1f))
    }

    @Test
    fun zoomedDiagramMovesUpToItsEdges() {
        assertOffset(Offset(1500f, 0f), Offset(5000f, 400f).limitedTo(viewport, diagram, scale = 4f))
        assertOffset(Offset(-1500f, 0f), Offset(-5000f, -400f).limitedTo(viewport, diagram, scale = 4f))
    }

    @Test
    fun tallDiagramMovesVerticallyWhenItOutgrowsScreen() {
        assertOffset(Offset(0f, 1000f), Offset(0f, 9000f).limitedTo(viewport, diagram, scale = 8f))
    }

    @Test
    fun zoomKeepsPointUnderFingers() {
        val centroid = Offset(800f, 1000f)

        assertOffset(Offset(-300f, 0f), Offset.Zero.zoomedAt(centroid, viewport, factor = 2f))
    }
}

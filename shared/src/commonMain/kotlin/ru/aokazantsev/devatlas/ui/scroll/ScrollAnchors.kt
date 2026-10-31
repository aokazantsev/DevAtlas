package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

class ScrollAnchors {
    private var content: LayoutCoordinates? = null
    private val items = mutableMapOf<Int, LayoutCoordinates>()

    val contentModifier: Modifier = Modifier.onGloballyPositioned { content = it }

    fun item(index: Int): Modifier = Modifier.onGloballyPositioned { items[index] = it }

    fun topOf(index: Int): Int? {
        val container = content?.takeIf { it.isAttached } ?: return null
        val item = items[index]?.takeIf { it.isAttached } ?: return null
        return container.localPositionOf(item, Offset.Zero).y.roundToInt()
    }

    fun heightOf(index: Int): Int? {
        val top = topOf(index) ?: return null
        return topOf(index + 1)?.minus(top)
    }

    fun topAtOrBefore(index: Int): Int? = (index downTo 0).firstNotNullOfOrNull(::topOf)

    fun firstVisible(scrollOffset: Int): Int? {
        val placed = items.keys.filter { topOf(it) != null }
        if (placed.isEmpty()) return null
        return placed.sortedDescending().firstOrNull { index -> topOf(index)?.let { it <= scrollOffset } == true } ?: 0
    }
}

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

    fun top(index: Int): Int? {
        val container = content?.takeIf { it.isAttached } ?: return null
        val item = items[index]?.takeIf { it.isAttached } ?: return null
        return container.localPositionOf(item, Offset.Zero).y.roundToInt()
    }
}

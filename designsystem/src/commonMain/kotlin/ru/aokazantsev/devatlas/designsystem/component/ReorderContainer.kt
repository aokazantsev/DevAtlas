package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

internal class ReorderContainer {
    var coordinates: LayoutCoordinates? = null

    fun boundsOf(item: LayoutCoordinates): ReorderBounds? {
        val parent = coordinates?.takeIf { it.isAttached } ?: return null
        if (!item.isAttached) return null
        val top = parent.localPositionOf(item, Offset.Zero).y
        return ReorderBounds(top = top, height = item.size.height.toFloat())
    }
}

package ru.aokazantsev.devatlas.designsystem.component

internal data class ReorderBounds(val top: Float, val height: Float) {
    operator fun contains(y: Float): Boolean = y >= top && y < top + height
}

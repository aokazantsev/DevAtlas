package ru.aokazantsev.devatlas.ui.web

import kotlin.math.hypot

internal class TapTracker(private val touchSlop: Float, private val longPressMillis: Long) {
    private var startX = 0f
    private var startY = 0f
    private var possible = false

    fun onDown(x: Float, y: Float) {
        startX = x
        startY = y
        possible = true
    }

    fun onMove(x: Float, y: Float) {
        if (moved(x, y)) possible = false
    }

    fun cancel() {
        possible = false
    }

    fun onUp(x: Float, y: Float, pressedMillis: Long): Boolean {
        val tap = possible && !moved(x, y) && pressedMillis < longPressMillis
        possible = false
        return tap
    }

    private fun moved(x: Float, y: Float): Boolean = hypot(x - startX, y - startY) > touchSlop
}

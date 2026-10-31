package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class ReorderState {
    private val bounds = mutableStateMapOf<Any, ReorderBounds>()
    private var order by mutableStateOf<List<Any>?>(null)
    private var heights: Map<Any, Float> = emptyMap()
    private var firstTop = 0f
    private var gap = 0f
    private var pointerY by mutableFloatStateOf(0f)
    private var grab = 0f
    private var startIndex = 0

    var dragged by mutableStateOf<Any?>(null)
        private set

    var settled by mutableStateOf<List<Any>?>(null)
        private set

    fun displayed(keys: List<Any>): List<Any> = when {
        dragged != null -> order ?: keys
        else -> settled?.takeIf { it.sameRowsAs(keys) } ?: keys
    }

    fun isAwaitingOwner(keys: List<Any>): Boolean = settled?.let { it != keys && it.sameRowsAs(keys) } == true

    fun release() {
        settled = null
    }

    fun place(key: Any, placed: ReorderBounds) {
        if (bounds[key] != placed) bounds[key] = placed
    }

    fun forget(key: Any) {
        bounds.remove(key)
    }

    fun translation(key: Any): Float {
        if (key != dragged) return 0f
        val top = bounds[key]?.top ?: return 0f
        return pointerY - grab - top
    }

    fun start(keys: List<Any>, y: Float): Boolean {
        val placed = keys.map { bounds[it] ?: return false }
        val index = placed.indexOfFirst { y in it }
        if (index < 0) return false
        order = keys
        heights = keys.zip(placed) { key, box -> key to box.height }.toMap()
        firstTop = placed.first().top
        gap = if (placed.size > 1) placed[1].top - placed[0].top - placed[0].height else 0f
        startIndex = index
        grab = y - placed[index].top
        pointerY = y
        settled = null
        dragged = keys[index]
        return true
    }

    fun dragTo(y: Float) {
        pointerY = y
        val key = dragged ?: return
        var current = order ?: return
        while (true) {
            val target = targetIndex(current, key, y) ?: break
            current = current.toMutableList().apply { add(target, removeAt(current.indexOf(key))) }
        }
        order = current
    }

    fun finish(commit: Boolean): Pair<Int, Int>? {
        val key = dragged ?: return null
        val finalOrder = order
        val to = finalOrder?.indexOf(key) ?: startIndex
        dragged = null
        order = null
        if (!commit || to == startIndex) return null
        settled = finalOrder
        return startIndex to to
    }

    private fun List<Any>.sameRowsAs(other: List<Any>): Boolean = size == other.size && toSet() == other.toSet()

    private fun targetIndex(current: List<Any>, key: Any, y: Float): Int? {
        val index = current.indexOf(key)
        val visualTop = y - grab
        val visualBottom = visualTop + heights.getValue(key)
        val hasNext = index + 1 < current.size
        return when {
            hasNext && visualBottom > slotCenter(current, index + 1) -> index + 1
            index > 0 && visualTop < slotCenter(current, index - 1) -> index - 1
            else -> null
        }
    }

    private fun slotCenter(current: List<Any>, index: Int): Float {
        val top = firstTop + current.take(index).fold(0f) { sum, key -> sum + heights.getValue(key) + gap }
        return top + heights.getValue(current[index]) / 2
    }
}

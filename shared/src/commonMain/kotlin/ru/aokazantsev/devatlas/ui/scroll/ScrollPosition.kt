package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull

@Stable
class ScrollPosition(anchor: Int, offset: Int = 0) {
    val scrollState = ScrollState(initial = 0)
    val anchors = ScrollAnchors()
    private val pending = mutableStateOf(
        ReadingPlace(anchor, offset.coerceAtLeast(0)).takeIf { anchor > 0 || offset > 0 },
    )
    private var lastVisible: ReadingPlace? = null

    val pendingPlace: ReadingPlace?
        get() = pending.value

    fun markRestored() {
        pending.value = null
    }

    fun current(): Int? = place()?.anchor

    suspend fun trackVisible() {
        snapshotFlow { scrollState.value.takeUnless { scrollState.isScrollInProgress } }
            .filterNotNull()
            .collect { visible() }
    }

    private fun place(): ReadingPlace? = pending.value ?: visible()

    private fun visible(): ReadingPlace? {
        val value = scrollState.value
        val anchor = anchors.firstVisible(value) ?: return lastVisible
        val offset = anchors.topOf(anchor)?.let { (value - it).coerceAtLeast(0) } ?: 0
        return ReadingPlace(anchor, offset).also { lastVisible = it }
    }

    companion object {
        val Saver: Saver<ScrollPosition, List<Int>> = Saver(
            save = { position -> position.place()?.let { listOf(it.anchor, it.offset) } ?: listOf(0, 0) },
            restore = { ScrollPosition(it[0], it[1]) },
        )
    }
}

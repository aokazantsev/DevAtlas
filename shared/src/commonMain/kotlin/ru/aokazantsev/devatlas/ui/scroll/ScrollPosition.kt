package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable

@Stable
class ScrollPosition(
    val scrollState: ScrollState,
    val anchors: ScrollAnchors,
    val initialAnchor: Int,
    private val restored: MutableState<Boolean>,
) {
    val isRestored: Boolean
        get() = restored.value

    fun markRestored() {
        restored.value = true
    }

    fun current(): Int? = if (restored.value) anchors.firstVisible(scrollState.value) else initialAnchor
}

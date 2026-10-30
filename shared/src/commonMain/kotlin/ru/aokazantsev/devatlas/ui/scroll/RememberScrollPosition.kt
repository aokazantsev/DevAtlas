package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun rememberScrollPosition(anchor: Int): ScrollPosition {
    val scrollState = rememberScrollState()
    val anchors = remember { ScrollAnchors() }
    val restored = rememberSaveable { mutableStateOf(anchor <= 0) }
    return remember(scrollState, anchors, anchor, restored) { ScrollPosition(scrollState, anchors, anchor, restored) }
}

package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

@Composable
fun RestoreAnchor(anchors: ScrollAnchors, scrollState: ScrollState, anchor: Int) {
    var restored by rememberSaveable { mutableStateOf(anchor <= 0) }
    LaunchedEffect(anchor) {
        if (restored) return@LaunchedEffect
        withFrameNanos { }
        anchors.top(anchor)?.let { scrollState.scrollTo(it) }
        restored = true
    }
}

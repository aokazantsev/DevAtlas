package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

@Composable
fun RestoreAnchor(position: ScrollPosition) {
    LaunchedEffect(position) {
        if (position.isRestored) return@LaunchedEffect
        withFrameNanos { }
        position.anchors.topAtOrBefore(position.initialAnchor)?.let { position.scrollState.scrollTo(it) }
        position.markRestored()
    }
}

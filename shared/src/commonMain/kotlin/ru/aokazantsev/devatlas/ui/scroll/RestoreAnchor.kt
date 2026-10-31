package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

@Composable
fun RestoreAnchor(position: ScrollPosition) {
    LaunchedEffect(position) {
        position.pendingPlace?.let { place ->
            withFrameNanos { }
            val anchors = position.anchors
            val offset = anchors.heightOf(place.anchor)?.let { place.offset.coerceAtMost(it) } ?: place.offset
            val target = anchors.topOf(place.anchor)?.plus(offset) ?: anchors.topAtOrBefore(place.anchor)
            target?.let { position.scrollState.scrollTo(it) }
            position.markRestored()
        }
        position.trackVisible()
    }
}

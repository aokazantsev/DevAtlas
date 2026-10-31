package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

@Composable
fun RestoreAnchor(position: ScrollPosition) {
    LaunchedEffect(position) {
        position.pendingPlace?.let { place ->
            withFrameNanos { }
            val target = position.anchors.topOf(place.anchor)?.plus(place.offset)
                ?: position.anchors.topAtOrBefore(place.anchor)
            target?.let { position.scrollState.scrollTo(it) }
            position.markRestored()
        }
        position.trackVisible()
    }
}

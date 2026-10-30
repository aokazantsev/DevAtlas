package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun RecordPosition(position: ScrollPosition, onRecord: (anchor: Int) -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { position.current()?.let(onRecord) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { position.current()?.let(onRecord) }
}

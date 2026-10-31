package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@Composable
fun <T> AtlasReorderableGroup(
    items: List<T>,
    itemKey: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (item: T, index: Int, count: Int, rowModifier: Modifier) -> Unit,
) {
    val state = remember { ReorderState() }
    val haptics = LocalHapticFeedback.current
    val currentOnMove by rememberUpdatedState(onMove)
    val byKey = items.associateBy(itemKey)
    val keys = items.map(itemKey)
    val currentKeys by rememberUpdatedState(keys)
    val shown = state.displayed(keys).filter { it in byKey }
    val container = remember { ReorderContainer() }
    LaunchedEffect(state.settled, keys) {
        if (state.settled == null) return@LaunchedEffect
        if (state.isAwaitingOwner(keys)) delay(OWNER_SETTLE_MILLIS)
        state.release()
    }
    AtlasListGroup(
        modifier = modifier
            .onGloballyPositioned { container.coordinates = it }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!awaitHold(down)) return@awaitEachGesture
                    if (!state.start(currentKeys, down.position.y)) return@awaitEachGesture
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var committed = false
                    try {
                        committed = followDrag(down, state)
                    } finally {
                        state.finish(committed)?.let { (from, to) -> currentOnMove(from, to) }
                    }
                }
            },
    ) {
        shown.forEachIndexed { index, rowKey ->
            val item = byKey.getValue(rowKey)
            key(rowKey) {
                DisposableEffect(rowKey) { onDispose { state.forget(rowKey) } }
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            container.boundsOf(coordinates)?.let { state.place(rowKey, it) }
                        }
                        .zIndex(if (state.dragged == rowKey) 1f else 0f)
                        .graphicsLayer {
                            translationY = state.translation(rowKey)
                            val scale = if (state.dragged == rowKey) DRAGGED_SCALE else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                    val actions = moveActions(index, shown.size) { from, to -> currentOnMove(from, to) }
                    row(item, index, shown.size, Modifier.semantics { customActions = actions })
                }
            }
        }
    }
}

private const val DRAGGED_SCALE = 1.03f
private const val OWNER_SETTLE_MILLIS = 300L

private fun moveActions(index: Int, count: Int, onMove: (Int, Int) -> Unit): List<CustomAccessibilityAction> = buildList {
    if (index > 0) add(CustomAccessibilityAction("Переместить выше") { onMove(index, index - 1); true })
    if (index < count - 1) add(CustomAccessibilityAction("Переместить ниже") { onMove(index, index + 1); true })
}

private suspend fun AwaitPointerEventScope.awaitHold(down: PointerInputChange): Boolean {
    val timeout = viewConfiguration.longPressTimeoutMillis
    var held = false
    val finished = withTimeoutOrNull(timeout) {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
            val lifted = change == null || !change.pressed || change.isConsumed
            if (!lifted && change.uptimeMillis - down.uptimeMillis >= timeout) held = true
            if (held || lifted || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
        }
    }
    return held || finished == null
}

private suspend fun AwaitPointerEventScope.followDrag(down: PointerInputChange, state: ReorderState): Boolean {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == down.id } ?: return false
        val cancelled = !change.pressed && change.isConsumed
        event.changes.forEach { it.consume() }
        if (cancelled) return false
        if (!change.pressed) return true
        state.dragTo(change.position.y)
    }
}

package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline

internal fun Modifier.linkLongPress(
    content: InlineContent,
    layout: () -> TextLayoutResult?,
    onLongPress: (PageInline.Link) -> Unit,
): Modifier = pointerInput(content, onLongPress) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val link = layout()?.linkAt(content, down.position) ?: return@awaitEachGesture
        val timeout = viewConfiguration.longPressTimeoutMillis
        val outcome = withTimeoutOrNull(timeout) { awaitHold(down, timeout, viewConfiguration.touchSlop) } ?: HoldOutcome.STILL_PRESSED
        if (outcome == HoldOutcome.NOT_HELD) return@awaitEachGesture
        onLongPress(link)
        if (outcome == HoldOutcome.STILL_PRESSED) consumeUntilUp(down)
    }
}

private fun TextLayoutResult.linkAt(content: InlineContent, position: Offset): PageInline.Link? {
    if (position.y < 0f || position.y > size.height) return null
    val line = getLineForVerticalPosition(position.y)
    if (position.x < getLineLeft(line) || position.x > getLineRight(line)) return null
    val offset = getOffsetForPosition(position)
    val under = if (offset > 0 && (offset >= content.text.length || !getBoundingBox(offset).contains(position))) offset - 1 else offset
    return content.text.linkIndexAt(under)?.let(content.links::getOrNull)
}

private suspend fun AwaitPointerEventScope.awaitHold(down: PointerInputChange, timeout: Long, slop: Float): HoldOutcome {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
            ?: return HoldOutcome.NOT_HELD
        val heldFor = change.uptimeMillis - down.uptimeMillis
        when {
            !change.pressed && heldFor >= timeout -> {
                change.consume()
                return HoldOutcome.RELEASED_AFTER_HOLD
            }
            !change.pressed -> return HoldOutcome.NOT_HELD
            (change.position - down.position).getDistance() > slop -> return HoldOutcome.NOT_HELD
            heldFor >= timeout -> return HoldOutcome.STILL_PRESSED
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilUp(down: PointerInputChange) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.id == down.id && it.pressed }) return
    }
}

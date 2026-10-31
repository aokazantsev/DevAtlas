package ru.aokazantsev.devatlas.ui.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TapTrackerTest {
    private val tracker = TapTracker(touchSlop = SLOP, longPressMillis = LONG_PRESS)

    @Test
    fun shortPressInPlaceIsTap() {
        tracker.onDown(0f, 0f)
        tracker.onMove(3f, 4f)

        assertTrue(tracker.onUp(3f, 4f, pressedMillis = 100))
    }

    @Test
    fun scrollIsNotTapEvenWhenFingerReturns() {
        tracker.onDown(0f, 0f)
        tracker.onMove(0f, 200f)

        assertFalse(tracker.onUp(0f, 0f, pressedMillis = 100))
    }

    @Test
    fun longPressIsNotTap() {
        tracker.onDown(0f, 0f)

        assertFalse(tracker.onUp(0f, 0f, pressedMillis = LONG_PRESS))
    }

    @Test
    fun cancelledGestureIsNotTap() {
        tracker.onDown(0f, 0f)
        tracker.cancel()

        assertFalse(tracker.onUp(0f, 0f, pressedMillis = 100))
    }

    @Test
    fun releaseFarAwayWithoutMoveIsNotTap() {
        tracker.onDown(0f, 0f)

        assertFalse(tracker.onUp(0f, 200f, pressedMillis = 100))
    }

    @Test
    fun pressJustShorterThanLongPressIsTap() {
        tracker.onDown(0f, 0f)

        assertTrue(tracker.onUp(0f, 0f, pressedMillis = LONG_PRESS - 1))
    }

    @Test
    fun newTouchAfterCancelIsTap() {
        tracker.onDown(0f, 0f)
        tracker.cancel()

        tracker.onDown(50f, 50f)

        assertTrue(tracker.onUp(50f, 50f, pressedMillis = 100))
    }

    @Test
    fun upWithoutDownIsNotTap() {
        assertFalse(tracker.onUp(0f, 0f, pressedMillis = 100))
    }

    private companion object {
        const val SLOP = 8f
        const val LONG_PRESS = 400L
    }
}

package ru.aokazantsev.devatlas.ui.web

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeProvider
import android.webkit.WebView

internal class AtlasWebView(context: Context, private val onUserInput: () -> Unit) : WebView(context) {
    private val taps = ViewConfiguration.get(context).let { config ->
        TapTracker(touchSlop = config.scaledTouchSlop.toFloat(), longPressMillis = ViewConfiguration.getLongPressTimeout().toLong())
    }
    private var reportingProvider: ClickReportingAccessibilityProvider? = null

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> taps.onDown(event.x, event.y)
            MotionEvent.ACTION_MOVE -> taps.onMove(event.x, event.y)
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> taps.cancel()
            MotionEvent.ACTION_UP -> if (taps.onUp(event.x, event.y, event.eventTime - event.downTime)) onUserInput()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && event.keyCode in ACTIVATION_KEYS) onUserInput()
        return super.dispatchKeyEvent(event)
    }

    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider? {
        val provider = super.getAccessibilityNodeProvider() ?: return null
        val cached = reportingProvider
        if (cached != null && cached.delegate === provider) return cached
        return ClickReportingAccessibilityProvider(provider, onUserInput).also { reportingProvider = it }
    }

    private companion object {
        val ACTIVATION_KEYS = setOf(
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_SPACE,
        )
    }
}

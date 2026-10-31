package ru.aokazantsev.devatlas.e2e

import android.os.SystemClock
import android.view.KeyEvent
import android.webkit.WebView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.platform.app.InstrumentationRegistry

fun pressEnterInWebView() {
    onView(isAssignableFrom(WebView::class.java)).perform(WebViewFocusAction())
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val downTime = SystemClock.uptimeMillis()
    instrumentation.sendKeySync(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
    SystemClock.sleep(KEY_HOLD_MILLIS)
    instrumentation.sendKeySync(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
}

private const val KEY_HOLD_MILLIS = 300L

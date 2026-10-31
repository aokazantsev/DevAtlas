package ru.aokazantsev.devatlas.e2e

import android.view.View
import android.webkit.WebView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher

class WebViewFocusAction : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

    override fun getDescription(): String = "передать фокус WebView"

    override fun perform(uiController: UiController, view: View) {
        view.requestFocus()
        uiController.loopMainThreadUntilIdle()
    }
}

package ru.aokazantsev.devatlas.e2e

import android.view.View
import android.webkit.WebView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import org.hamcrest.Matcher

class WebViewLoadUrlAction(private val url: String) : ViewAction {
    override fun getConstraints(): Matcher<View> = isAssignableFrom(WebView::class.java)

    override fun getDescription(): String = "открыть $url в WebView"

    override fun perform(uiController: UiController, view: View) {
        (view as WebView).loadUrl(url)
        uiController.loopMainThreadUntilIdle()
    }
}

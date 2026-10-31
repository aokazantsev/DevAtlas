package ru.aokazantsev.devatlas.ui.web

import android.os.Bundle
import android.os.SystemClock
import android.webkit.WebView

internal class WebViewHolder(var saved: Bundle?) {
    var webView: WebView? = null
    var pageShown: Boolean = saved?.getBoolean(PAGE_SHOWN_KEY) == true
    val activation = UserActivation(now = SystemClock::uptimeMillis)

    fun save(): Bundle = Bundle().also { bundle ->
        val shown = webView
        if (shown != null) shown.saveState(bundle) else saved?.let(bundle::putAll)
        bundle.putBoolean(PAGE_SHOWN_KEY, pageShown)
    }

    fun pdfAllowed(url: String): Boolean = !pageShown || activation.consume(url)

    fun releaseAfterRenderGone(view: WebView) {
        saved = runCatching { Bundle().also { view.saveState(it) } }.getOrNull()
        webView = null
    }

    private companion object {
        const val PAGE_SHOWN_KEY = "devatlas.pageShown"
    }
}

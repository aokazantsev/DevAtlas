package ru.aokazantsev.devatlas.ui.web

import android.os.Bundle
import android.webkit.WebView

internal class WebViewHolder(val saved: Bundle?) {
    var webView: WebView? = null
    var pageShown: Boolean = saved?.getBoolean(PAGE_SHOWN_KEY) == true

    fun save(): Bundle = Bundle().also { bundle ->
        webView?.saveState(bundle)
        bundle.putBoolean(PAGE_SHOWN_KEY, pageShown)
    }

    private companion object {
        const val PAGE_SHOWN_KEY = "devatlas.pageShown"
    }
}

package ru.aokazantsev.devatlas.ui.web

import android.graphics.Bitmap
import android.security.NetworkSecurityPolicy
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import ru.aokazantsev.devatlas.files.domain.PdfFileNames
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest

internal class AtlasWebViewClient(
    private val state: WebPageState,
    private val holder: WebViewHolder,
    private val callbacks: () -> WebCallbacks,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        val url = request.url.toString()
        val scheme = request.url.scheme?.lowercase()
        val asked = request.hasGesture() || !holder.pageShown
        val handled = when {
            scheme != "http" && scheme != "https" -> {
                if (asked && scheme !in LOCAL_SCHEMES) callbacks().onExternal(url)
                true
            }
            scheme == "http" && !cleartextAllowed(request.url.host.orEmpty()) -> {
                if (asked) callbacks().onExternal(url)
                true
            }
            PdfFileNames.isPdfUrl(url) -> {
                callbacks().onPdf(PdfRequest(url = url, title = state.title))
                true
            }
            else -> false
        }
        if (handled && !holder.pageShown) callbacks().onNothingShown()
        return handled
    }

    private fun cleartextAllowed(host: String): Boolean = NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        state.url = url
        state.failed = false
    }

    override fun onPageCommitVisible(view: WebView, url: String?) {
        if (!state.failed) holder.pageShown = true
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (!state.failed) holder.pageShown = true
        state.url = url
        state.title = view.pageTitle()
        state.canGoBack = view.canGoBack()
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        state.url = url
        state.title = view.pageTitle()
        state.canGoBack = view.canGoBack()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) state.failed = true
    }

    private companion object {
        val LOCAL_SCHEMES = setOf("data", "blob", "javascript", "file", "content", "about")
    }
}

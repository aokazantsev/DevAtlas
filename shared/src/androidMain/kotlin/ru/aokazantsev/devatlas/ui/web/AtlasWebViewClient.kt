package ru.aokazantsev.devatlas.ui.web

import android.graphics.Bitmap
import android.net.Uri
import android.security.NetworkSecurityPolicy
import android.webkit.WebResourceError
import android.webkit.RenderProcessGoneDetail
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
        holder.activation.onNavigation(url, hasGesture = request.hasGesture(), isRedirect = request.isRedirect)
        val handled = intercept(request.url, asked = request.hasGesture() || !holder.pageShown)
        if (handled && !holder.pageShown) callbacks().onNothingShown()
        return handled
    }

    private fun intercept(uri: Uri, asked: Boolean): Boolean {
        val url = uri.toString()
        val scheme = uri.scheme?.lowercase()
        return when {
            scheme != "http" && scheme != "https" -> {
                if (asked && scheme !in LOCAL_SCHEMES) callbacks().onExternal(url)
                true
            }
            scheme == "http" && !cleartextAllowed(uri.host.orEmpty()) -> {
                if (asked) callbacks().onExternal(url)
                true
            }
            PdfFileNames.isPdfUrl(url) -> {
                if (holder.pdfAllowed(url)) callbacks().onPdf(PdfRequest(url = url, title = state.title))
                true
            }
            else -> false
        }
    }

    private fun cleartextAllowed(host: String): Boolean = NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        state.url = url
        state.failed = false
        holder.activation.onPageStarted()
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

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail?): Boolean {
        if (holder.webView === view) {
            holder.releaseAfterRenderGone(view)
            state.onRenderGone()
        }
        return true
    }

    private companion object {
        val LOCAL_SCHEMES = setOf("data", "blob", "javascript", "file", "content", "about")
    }
}

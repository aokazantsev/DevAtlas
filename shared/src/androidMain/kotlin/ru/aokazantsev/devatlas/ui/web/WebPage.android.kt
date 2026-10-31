package ru.aokazantsev.devatlas.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.aokazantsev.devatlas.files.domain.PdfFileNames
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest

@Composable
actual fun WebPage(
    startUrl: String,
    state: WebPageState,
    onPdf: (PdfRequest) -> Unit,
    onExternal: (String) -> Unit,
    onNothingShown: () -> Unit,
    modifier: Modifier,
) {
    val holder = rememberSaveable(saver = WebViewHolderSaver) { WebViewHolder(saved = null) }
    val callbacks = rememberUpdatedState(WebCallbacks(onPdf, onExternal, onNothingShown))
    BackHandler(enabled = state.canGoBack) { state.goBack() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, holder) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                holder.webView?.onPause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                holder.webView?.onResume()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (!state.renderGone) {
        AndroidView(
            factory = { context -> createWebView(context, startUrl, state, holder, callbacks::value) },
            modifier = modifier,
            onRelease = { webView ->
                if (holder.webView === webView) {
                    holder.webView = null
                    state.navigator = null
                }
                webView.destroy()
            },
        )
    }
}

private fun createWebView(
    context: Context,
    startUrl: String,
    state: WebPageState,
    holder: WebViewHolder,
    callbacks: () -> WebCallbacks,
): WebView = AtlasWebView(context, onUserInput = holder.activation::onUserInput).also { webView ->
    holder.webView = webView
    webView.configure(state, holder, callbacks)
    state.navigator = object : WebNavigator {
        override fun goBack() {
            if (webView.canGoBack()) webView.goBack()
        }

        override fun reload() {
            if (webView.url == null) webView.loadUrl(startUrl) else webView.reload()
        }
    }
    val saved = holder.saved
    if (saved == null || saved.isEmpty || webView.restoreState(saved) == null) webView.loadUrl(state.url ?: startUrl)
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configure(state: WebPageState, holder: WebViewHolder, callbacks: () -> WebCallbacks) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.setSupportMultipleWindows(false)
    webViewClient = AtlasWebViewClient(state, holder, callbacks)
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            state.progress = if (newProgress in 0 until FULL_PROGRESS) newProgress / FULL_PROGRESS.toFloat() else null
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            state.title = view.pageTitle()
        }
    }
    setDownloadListener { url, _, contentDisposition, mimeType, _ ->
        val isPdf = mimeType.orEmpty().contains(PDF_SUBTYPE, ignoreCase = true) ||
            PdfFileNames.fromDisposition(contentDisposition)?.endsWith(".$PDF_SUBTYPE", ignoreCase = true) == true ||
            PdfFileNames.isPdfUrl(url)
        val web = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
        when {
            isPdf -> if (holder.pdfAllowed(url)) {
                callbacks().onPdf(PdfRequest(url = url, title = state.title, contentDisposition = contentDisposition))
            }
            web && !holder.pageShown -> callbacks().onExternal(url)
        }
        if (!holder.pageShown) callbacks().onNothingShown()
    }
}

private val WebViewHolderSaver = Saver<WebViewHolder, Bundle>(
    save = { holder -> holder.save() },
    restore = { bundle -> WebViewHolder(saved = bundle) },
)

private const val FULL_PROGRESS = 100
private const val PDF_SUBTYPE = "pdf"

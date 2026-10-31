package ru.aokazantsev.devatlas.ui.web

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class WebPageState {
    var url by mutableStateOf<String?>(null)
        internal set

    var title by mutableStateOf<String?>(null)
        internal set

    var progress by mutableStateOf<Float?>(null)
        internal set

    var failed by mutableStateOf(false)
        internal set

    var renderGone by mutableStateOf(false)
        private set

    var canGoBack by mutableStateOf(false)
        internal set

    internal var navigator: WebNavigator? = null

    fun goBack() {
        navigator?.goBack()
    }

    fun reload() {
        failed = false
        if (renderGone) {
            renderGone = false
        } else {
            navigator?.reload()
        }
    }

    internal fun onRenderGone() {
        renderGone = true
        failed = false
        progress = null
        canGoBack = false
        navigator = null
    }
}

package ru.aokazantsev.devatlas.ui.web

import android.webkit.WebView

internal fun WebView.pageTitle(): String? = title?.takeIf { it.isNotBlank() && it != url }

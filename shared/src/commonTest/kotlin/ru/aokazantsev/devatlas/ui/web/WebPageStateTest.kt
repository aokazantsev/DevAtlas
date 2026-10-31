package ru.aokazantsev.devatlas.ui.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebPageStateTest {

    @Test
    fun renderGoneDropsWebViewAndRetryCreatesNewOne() {
        val navigator = CountingWebNavigator()
        val state = WebPageState().apply {
            this.navigator = navigator
            progress = 0.5f
            canGoBack = true
        }

        state.onRenderGone()

        assertTrue(state.renderGone)
        assertNull(state.progress)
        assertFalse(state.canGoBack)
        assertNull(state.navigator)

        state.reload()

        assertFalse(state.renderGone)
        assertEquals(1, state.webViewGeneration)
        assertEquals(0, navigator.reloads)
    }

    @Test
    fun networkFailureRetryReloadsSameWebView() {
        val navigator = CountingWebNavigator()
        val state = WebPageState().apply {
            this.navigator = navigator
            failed = true
        }

        state.reload()

        assertFalse(state.failed)
        assertEquals(0, state.webViewGeneration)
        assertEquals(1, navigator.reloads)
    }
}

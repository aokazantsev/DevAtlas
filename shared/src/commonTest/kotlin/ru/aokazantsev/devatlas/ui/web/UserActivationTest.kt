package ru.aokazantsev.devatlas.ui.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserActivationTest {
    private var time = 0L
    private val activation = UserActivation(now = { time })

    @Test
    fun nothingIsAllowedWithoutInput() {
        assertFalse(activation.consume(PDF))
    }

    @Test
    fun navigationMarkedAsGestureByBrowserGrantsNothingWithoutInput() {
        activation.onNavigation(PDF, hasGesture = true, isRedirect = false)

        assertFalse(activation.consume(PDF))
    }

    @Test
    fun oneInputAllowsExactlyOneDownload() {
        activation.onUserInput()

        assertTrue(activation.consume(PDF))
        assertFalse(activation.consume(OTHER))
    }

    @Test
    fun scriptNavigationAfterSpentInputGrantsNothing() {
        activation.onUserInput()
        activation.onNavigation(PDF, hasGesture = true, isRedirect = false)
        assertTrue(activation.consume(PDF))

        activation.onNavigation(OTHER, hasGesture = true, isRedirect = false)

        assertFalse(activation.consume(OTHER))
    }

    @Test
    fun inputNavigationAllowsOnlyItsOwnTarget() {
        activation.onUserInput()
        activation.onNavigation(PAGE, hasGesture = true, isRedirect = false)
        activation.onNavigation(OTHER, hasGesture = true, isRedirect = false)

        assertFalse(activation.consume(OTHER))
        assertTrue(activation.consume(PAGE))
        assertFalse(activation.consume(PAGE))
    }

    @Test
    fun targetIsComparedWithoutFragment() {
        activation.onUserInput()
        activation.onNavigation("$PDF#page=2", hasGesture = true, isRedirect = false)

        assertTrue(activation.consume(PDF))
    }

    @Test
    fun redirectInheritsUnspentInput() {
        activation.onUserInput()
        activation.onNavigation(PAGE, hasGesture = true, isRedirect = false)
        activation.onNavigation(PDF, hasGesture = false, isRedirect = true)

        assertTrue(activation.consume(PDF))
    }

    @Test
    fun redirectWithoutInputGrantsNothing() {
        activation.onNavigation(PDF, hasGesture = true, isRedirect = true)

        assertFalse(activation.consume(PDF))
    }

    @Test
    fun navigationWithoutBrowserGestureStillUsesInput() {
        activation.onUserInput()
        activation.onNavigation(PAGE, hasGesture = false, isRedirect = false)

        assertTrue(activation.consume(PAGE))
    }

    @Test
    fun slowServerWithinWindowIsAllowed() {
        activation.onUserInput()
        activation.onNavigation(PAGE, hasGesture = true, isRedirect = false)
        time += SLOW_SERVER_MILLIS

        assertTrue(activation.consume(PAGE))
    }

    @Test
    fun inputExpiresAfterWindow() {
        activation.onUserInput()
        time += UserActivation.WINDOW_MILLIS + 1

        assertFalse(activation.consume(PDF))
    }

    @Test
    fun shownPageSpendsInput() {
        activation.onUserInput()
        activation.onNavigation(PAGE, hasGesture = true, isRedirect = false)
        activation.onPageStarted()

        assertFalse(activation.consume(PAGE))
        assertFalse(activation.consume(PDF))
    }

    @Test
    fun newInputReplacesBoundTarget() {
        activation.onUserInput()
        activation.onNavigation(PAGE, hasGesture = true, isRedirect = false)
        activation.onUserInput()

        assertTrue(activation.consume(PDF))
    }

    private companion object {
        const val PDF = "https://site.test/files/guide.pdf"
        const val PAGE = "https://site.test/files/report"
        const val OTHER = "https://site.test/files/other.pdf"
        const val SLOW_SERVER_MILLIS = 30_000L
    }
}

package ru.aokazantsev.devatlas.ui.web

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClickReportingAccessibilityProviderTest {
    private val events = mutableListOf<String>()
    private val delegate = RecordingNodeProvider(events)
    private val provider = ClickReportingAccessibilityProvider(delegate) { events += CLICK_REPORTED }

    @Test
    fun clickIsReportedBeforeDelegateHandlesIt() {
        provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_CLICK, null)

        assertEquals(listOf(CLICK_REPORTED, "action ${AccessibilityNodeInfo.ACTION_CLICK} on $VIRTUAL_ID"), events)
    }

    @Test
    fun otherActionsAreNotReported() {
        provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
        provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_LONG_CLICK, null)
        provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)

        assertFalse(CLICK_REPORTED in events)
        assertEquals(3, events.size)
    }

    @Test
    fun delegateResultIsReturned() {
        delegate.result = true
        assertTrue(provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_CLICK, null))

        delegate.result = false
        assertFalse(provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_CLICK, null))
    }

    @Test
    fun argumentsReachDelegate() {
        val arguments = Bundle()

        provider.performAction(VIRTUAL_ID, AccessibilityNodeInfo.ACTION_CLICK, arguments)

        assertTrue(delegate.lastArguments === arguments)
    }

    private companion object {
        const val VIRTUAL_ID = 7
        const val CLICK_REPORTED = "click reported"
    }
}

package ru.aokazantsev.devatlas.ui.web

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeProvider

class RecordingNodeProvider(private val events: MutableList<String>) : AccessibilityNodeProvider() {
    var result = true
    var lastArguments: Bundle? = null

    override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        events += "action $action on $virtualViewId"
        lastArguments = arguments
        return result
    }
}

package ru.aokazantsev.devatlas.ui.web

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider

internal class ClickReportingAccessibilityProvider(
    val delegate: AccessibilityNodeProvider,
    private val onClick: () -> Unit,
) : AccessibilityNodeProvider() {

    override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? =
        delegate.createAccessibilityNodeInfo(virtualViewId)

    override fun addExtraDataToAccessibilityNodeInfo(
        virtualViewId: Int,
        info: AccessibilityNodeInfo,
        extraDataKey: String,
        arguments: Bundle?,
    ) {
        delegate.addExtraDataToAccessibilityNodeInfo(virtualViewId, info, extraDataKey, arguments)
    }

    override fun findAccessibilityNodeInfosByText(text: String, virtualViewId: Int): MutableList<AccessibilityNodeInfo>? =
        delegate.findAccessibilityNodeInfosByText(text, virtualViewId)

    override fun findFocus(focus: Int): AccessibilityNodeInfo? = delegate.findFocus(focus)

    override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_CLICK) onClick()
        return delegate.performAction(virtualViewId, action, arguments)
    }
}

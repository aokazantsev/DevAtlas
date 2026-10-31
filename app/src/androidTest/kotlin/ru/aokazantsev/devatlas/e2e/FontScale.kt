package ru.aokazantsev.devatlas.e2e

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

const val LARGE_FONT_SCALE = "2.0"
private const val FONT_WAIT_MILLIS = 10_000L

fun DevAtlasComposeRule.withFontScale(scale: String, block: () -> Unit) {
    val previousScale = shell("settings get system font_scale").trim()
    shell("settings put system font_scale $scale")
    try {
        waitUntil(FONT_WAIT_MILLIS) { activityFontScale() == scale.toFloat() }
        block()
    } finally {
        if (previousScale.isEmpty() || previousScale == "null") {
            shell("settings delete system font_scale")
        } else {
            shell("settings put system font_scale $previousScale")
        }
    }
}

private fun DevAtlasComposeRule.activityFontScale(): Float {
    var scale = 0f
    activityRule.scenario.onActivity { scale = it.resources.configuration.fontScale }
    return scale
}

private fun shell(command: String): String =
    ParcelFileDescriptor.AutoCloseInputStream(
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command),
    ).use { it.readBytes().decodeToString() }

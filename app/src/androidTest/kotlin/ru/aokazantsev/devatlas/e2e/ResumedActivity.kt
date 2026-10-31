package ru.aokazantsev.devatlas.e2e

import android.app.Activity
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

fun <T : Any> onResumedActivity(block: (Activity) -> T): T? {
    var result: T? = null
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        result = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()?.let(block)
    }
    return result
}

fun requestOrientation(orientation: Int) {
    onResumedActivity { it.requestedOrientation = orientation }
}

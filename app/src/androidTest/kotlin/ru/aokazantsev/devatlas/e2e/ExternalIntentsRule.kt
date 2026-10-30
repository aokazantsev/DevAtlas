package ru.aokazantsev.devatlas.e2e

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.ComponentNameMatchers.hasPackageName
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.rules.ExternalResource

class ExternalIntentsRule : ExternalResource() {

    override fun before() {
        Intents.init()
        val testPackage = InstrumentationRegistry.getInstrumentation().context.packageName
        intending(allOf(not(isInternal()), not(hasComponent(hasPackageName(testPackage)))))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    override fun after() {
        Intents.release()
    }
}

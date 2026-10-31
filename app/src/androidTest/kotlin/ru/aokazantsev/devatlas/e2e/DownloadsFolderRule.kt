package ru.aokazantsev.devatlas.e2e

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import java.io.FileInputStream

class DownloadsFolderRule : ExternalResource() {

    override fun before() {
        clear()
    }

    override fun after() {
        clear()
    }

    private fun clear() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("rm -rf $FOLDER").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
    }

    private companion object {
        const val FOLDER = "/sdcard/Download/DevAtlas"
    }
}

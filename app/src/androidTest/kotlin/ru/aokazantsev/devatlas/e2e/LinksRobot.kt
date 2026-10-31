package ru.aokazantsev.devatlas.e2e

import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

private const val INTENT_TIMEOUT_MILLIS = 15_000L

val pdfOpened: Matcher<Intent> = allOf(hasAction(Intent.ACTION_VIEW), hasType("application/pdf"))

fun ComposeTestRule.openLinksPage() {
    clickText("Ссылки и файлы")
    clickText("Сайты и PDF")
    waitForTextContaining("Сайт:")
}

fun ComposeTestRule.longClickLink(linkText: String) {
    val paragraph = hasText(linkText, substring = true) and hasAnyAncestor(hasScrollAction())
    waitFor(paragraph)
    val node = onNode(paragraph).performScrollTo()
    val layouts = mutableListOf<TextLayoutResult>()
    node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
    val layout = layouts.first()
    val start = layout.layoutInput.text.text.indexOf(linkText)
    val box = layout.getBoundingBox(start)
    node.performTouchInput { longClick(box.center) }
}

fun ComposeTestRule.waitForIntents(matcher: Matcher<Intent>, count: Int) {
    waitUntil(INTENT_TIMEOUT_MILLIS) { Intents.getIntents().count(matcher::matches) >= count }
    val actual = Intents.getIntents().count(matcher::matches)
    check(actual == count) { "Ожидалось интентов: $count, было: $actual" }
}

fun ComposeTestRule.openDownloads() {
    goBackToContents()
    openSettings()
    clickText("Скачанные файлы")
    waitFor(hasText("Скачанные файлы"))
}

fun ComposeTestRule.clickDescription(description: String) {
    waitFor(hasContentDescription(description))
    onNodeWithContentDescription(description).performClick()
}

fun ComposeTestRule.assertNoDescription(description: String) {
    check(onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isEmpty()) { "«$description» не должно быть на экране" }
}

fun clipboardText(): String? {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var text: String? = null
    instrumentation.runOnMainSync {
        val clipboard = instrumentation.targetContext.getSystemService(ClipboardManager::class.java)
        text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
    }
    return text
}

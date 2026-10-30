package ru.aokazantsev.devatlas.e2e

import android.content.ClipboardManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.SpanStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.aokazantsev.devatlas.ui.page.code.CODE_BLOCK_TAG

@RunWith(AndroidJUnit4::class)
class CodeE2eTest {

    private val e2e = DevAtlasE2e()

    @get:Rule
    val rules = e2e.chain

    private val app get() = e2e.compose

    private fun openCodePage() {
        app.passOnboarding()
        app.clickText("Текст")
        app.clickText("Код и таблицы")
        app.clickText("Код")
        app.waitForTextContaining(KOTLIN_CODE)
    }

    private fun codeStyles(fragment: String): List<SpanStyle> {
        val node = app.onNode(hasText(fragment, substring = true) and hasAnyAncestor(hasTestTag(CODE_BLOCK_TAG))).fetchSemanticsNode()
        return node.config[SemanticsProperties.Text].flatMap { text -> text.spanStyles.map { it.item } }
    }

    private fun clipboardText(): String? {
        var text: String? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
            text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
        }
        return text
    }

    @Test
    fun codeBlocksAreHighlightedAndLabeledWithLanguage() {
        openCodePage()

        app.waitForText("Kotlin")
        assertTrue(codeStyles(KOTLIN_CODE).isNotEmpty())
        app.waitForText("Текст")
        assertTrue(codeStyles("tree.svg").isEmpty())
        app.waitForText("Diff")
        assertTrue(codeStyles("+++ b/manifest.json").isNotEmpty())
    }

    @Test
    fun copyPutsExactBlockCodeToClipboard() {
        openCodePage()
        val copy = hasText("Копировать") and
            hasAnyAncestor(hasTestTag(CODE_BLOCK_TAG) and hasAnyDescendant(hasText(KOTLIN_CODE, substring = true)))

        app.onNode(copy).performScrollTo().performClick()

        app.waitForText("Скопировано")
        assertEquals(KOTLIN_BLOCK, clipboardText())
        app.waitForGone("Скопировано")
        app.waitFor(copy)
    }

    private companion object {
        const val KOTLIN_CODE = "fun Greeting(name: String)"
        const val KOTLIN_BLOCK = "@Composable\nfun Greeting(name: String) {\n    Text(text = \"Привет, \$name!\")\n}"
    }
}

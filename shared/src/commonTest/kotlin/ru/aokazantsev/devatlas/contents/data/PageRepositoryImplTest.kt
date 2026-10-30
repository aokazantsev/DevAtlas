package ru.aokazantsev.devatlas.contents.data

import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themes.FakeThemeStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PageRepositoryImplTest {

    private val fileSystem = FakeFileSystem()
    private val storage = FakeThemeStorage()
    private val repository = PageRepositoryImpl(storage, fileSystem)
    private val theme = storage.themeDirectory("t")
    private val page = ThemeNode.Page(id = "p", title = "P", file = "pages/p.html")

    private fun write(relative: String, text: String) {
        val path = theme / relative
        fileSystem.createDirectories(path.parent!!)
        fileSystem.write(path) { writeUtf8(text) }
    }

    @Test
    fun pageIsParsedWithItsDiagrams() = runTest {
        write(
            "pages/p.html",
            "${Char(BYTE_ORDER_MARK)}<p>Текст</p><ul><li><figure><img src=\"../d/a.svg\" alt=\"A\"></figure></li></ul>" +
                "<blockquote><img src=\"../d/b.svg\" alt=\"B\"></blockquote>",
        )
        write("d/a.svg", "<svg/>")
        write("d/b.svg", "<svg>b</svg>")

        val content = repository.load("t", page)!!

        assertEquals(PageBlock.Paragraph::class, content.document.blocks.first()::class)
        assertEquals(mapOf("d/a.svg" to "<svg/>", "d/b.svg" to "<svg>b</svg>"), content.diagrams)
    }

    @Test
    fun missingDiagramIsLeftOut() = runTest {
        write("pages/p.html", "<img src=\"../d/a.svg\" alt=\"A\">")

        assertEquals(emptyMap(), repository.load("t", page)!!.diagrams)
    }

    @Test
    fun diagramOverRenderBudgetIsLeftOut() = runTest {
        write("pages/p.html", "<img src=\"../d/a.svg\" alt=\"A\">")
        write("d/a.svg", "<svg viewBox=\"0 0 1 1\"><g id=\"a\"><use href=\"#a\"/></g></svg>")

        assertEquals(emptyMap(), repository.load("t", page)!!.diagrams)
    }

    @Test
    fun missingPageFileIsUnreadable() = runTest {
        assertNull(repository.load("t", page))
    }

    @Test
    fun pathOutsideThemeIsNotRead() = runTest {
        assertNull(repository.load("t", page.copy(file = "../escape.html")))
    }

    private companion object {
        const val BYTE_ORDER_MARK = 0xFEFF
    }
}

package ru.aokazantsev.devatlas.navigation

import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingStackTest {

    private val tree = listOf(
        ThemeNode.Page("intro", "Вступление", "intro.html"),
        ThemeNode.Section(
            "basics",
            "Основы",
            listOf(
                ThemeNode.Page("a", "A", "a.html"),
                ThemeNode.Section("deep", "Глубже", listOf(ThemeNode.Page("b", "B", "b.html"), ThemeNode.Page("c", "C", "c.html"))),
            ),
        ),
    )

    @Test
    fun rootIsSingleScreen() {
        assertEquals(listOf(Route.Contents(3)), readingStack(tree, ScreenTarget.Root, 3))
    }

    @Test
    fun topLevelPageHasOnlyContentsBelow() {
        assertEquals(listOf(Route.Contents(0), Route.Page("intro", 2)), readingStack(tree, ScreenTarget.Page("intro"), 2))
    }

    @Test
    fun deepPageGoesThroughEverySectionPointingAtChildRow() {
        assertEquals(
            listOf(Route.Contents(1), Route.Section("basics", 1), Route.Section("deep", 1), Route.Page("c", 7)),
            readingStack(tree, ScreenTarget.Page("c"), 7),
        )
    }

    @Test
    fun sectionTargetKeepsItsOwnAnchor() {
        assertEquals(
            listOf(Route.Contents(1), Route.Section("basics", 1), Route.Section("deep", 4)),
            readingStack(tree, ScreenTarget.Section("deep"), 4),
        )
    }

    @Test
    fun kindMustMatch() {
        assertNull(readingStack(tree, ScreenTarget.Page("deep"), 0))
        assertNull(readingStack(tree, ScreenTarget.Section("a"), 0))
    }

    @Test
    fun missingNodeHasNoStack() {
        assertNull(readingStack(tree, ScreenTarget.Page("gone"), 0))
    }
}

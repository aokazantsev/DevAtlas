package ru.aokazantsev.devatlas.themeformat.manifest

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ManifestStrictnessTest {

    private fun manifest(version: String, tree: String) =
        """{ "formatVersion": $version, "id": "demo", "title": "Демо", "tree": $tree }"""

    private val onePage = """[ { "id": "a", "title": "A", "file": "a.html" } ]"""

    @Test
    fun formatVersionInQuotesIsRejected() {
        val result = ManifestParser.parse(manifest("\"1\"", onePage))

        assertNull(result.manifest)
        assertEquals(listOf(IssueCode.MANIFEST_FIELD), result.issues.map { it.code })
    }

    @Test
    fun formatVersionNumberIsAccepted() {
        assertNotNull(ManifestParser.parse(manifest("1", onePage)).manifest)
    }

    @Test
    fun oneBrokenNodeKeepsTheRestOfTheTreeAndReportsEverything() {
        val result = ManifestParser.parse(
            manifest(
                "1",
                """[ { "id": "bad", "title": "X" },
                     { "id": "ok", "title": "Ok", "file": "ok.html" },
                     { "id": "ok", "title": "Дубль", "file": "dup.html" } ]""",
            ),
        )

        assertEquals(listOf(IssueCode.NODE_SHAPE, IssueCode.ID_DUPLICATE), result.issues.map { it.code })
        assertEquals(listOf("ok", "ok"), result.manifest?.pages?.map { it.id })
    }

    @Test
    fun brokenChildKeepsSectionWithTheOtherChildren() {
        val result = ManifestParser.parse(
            manifest(
                "1",
                """[ { "id": "s", "title": "S", "children": [ { "id": "x", "title": "X" },""" +
                    """ { "id": "ok", "title": "Ok", "file": "ok.html" } ] } ]""",
            ),
        )

        assertEquals(listOf("ok"), result.manifest?.pages?.map { it.id })
        assertEquals(listOf(IssueCode.NODE_SHAPE), result.issues.map { it.code })
    }

    @Test
    fun sectionWithOnlyBrokenChildrenDisappears() {
        val result = ManifestParser.parse(
            manifest(
                "1",
                """[ { "id": "s", "title": "S", "children": [ { "id": "x", "title": "X" } ] },""" +
                    """ { "id": "ok", "title": "Ok", "file": "ok.html" } ]""",
            ),
        )

        assertEquals(listOf("ok"), result.manifest?.tree?.map { it.id })
    }

    @Test
    fun allNodesBrokenGiveNoManifest() {
        val result = ManifestParser.parse(manifest("1", """[ { "id": "a", "title": "A" } ]"""))

        assertNull(result.manifest)
        assertEquals(listOf(IssueCode.NODE_SHAPE), result.issues.map { it.code })
    }
}

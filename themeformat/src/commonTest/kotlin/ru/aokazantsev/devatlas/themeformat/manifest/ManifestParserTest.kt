package ru.aokazantsev.devatlas.themeformat.manifest

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManifestParserTest {

    private fun manifest(tree: String, extra: String = "") =
        """{ "formatVersion": 1, "id": "demo", "title": "Демо", $extra "tree": $tree }"""

    private fun codes(source: String) = ManifestParser.parse(source).issues.map { it.code }

    @Test
    fun validTreeIsParsed() {
        val result = ManifestParser.parse(
            manifest(
                """[ { "id": "a", "title": "A", "file": "a.html" },
                     { "id": "s", "title": "S", "children": [ { "id": "b", "title": "B", "file": "pages/b.html" } ] } ]""",
                extra = """"description": "Описание",""",
            ),
        )

        val manifest = assertNotNull(result.manifest)
        assertEquals("demo", manifest.id)
        assertEquals("Описание", manifest.description)
        assertEquals(listOf("a", "b"), manifest.pages.map { it.id })
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun syntaxErrorHasPosition() {
        val result = ManifestParser.parse("{\n  \"id\": \"demo\",\n  oops\n}")

        assertNull(result.manifest)
        val issue = result.issues.single()
        assertEquals(IssueCode.MANIFEST_SYNTAX, issue.code)
        assertEquals(3, issue.line)
    }

    @Test
    fun missingAndWrongFieldsAreReported() {
        val result = ManifestParser.parse("""{ "id": 5, "tree": [] }""")

        assertNull(result.manifest)
        assertEquals(List(4) { IssueCode.MANIFEST_FIELD }, result.issues.map { it.code })
    }

    @Test
    fun formatVersionMustMatch() {
        assertEquals(
            listOf(IssueCode.FORMAT_VERSION),
            codes("""{ "formatVersion": 2, "id": "demo", "title": "Д", "tree": [ { "id": "a", "title": "A", "file": "a.html" } ] }"""),
        )
    }

    @Test
    fun idsAreCheckedForFormatAndUniqueness() {
        assertEquals(
            listOf(IssueCode.ID_INVALID, IssueCode.ID_DUPLICATE),
            codes(
                manifest(
                    """[ { "id": "Bad Id", "title": "A", "file": "a.html" },
                         { "id": "x", "title": "B", "file": "b.html" },
                         { "id": "x", "title": "C", "file": "c.html" } ]""",
                ),
            ),
        )
    }

    @Test
    fun nodeNeedsExactlyFileOrChildren() {
        assertEquals(listOf(IssueCode.NODE_SHAPE), codes(manifest("""[ { "id": "a", "title": "A" } ]""")))
        assertEquals(
            listOf(IssueCode.NODE_SHAPE),
            codes(manifest("""[ { "id": "a", "title": "A", "file": "a.html", "children": [] } ]""")),
        )
    }

    @Test
    fun pageFilePathsAreChecked() {
        assertEquals(listOf(IssueCode.PAGE_FILE_PATH), codes(manifest("""[ { "id": "a", "title": "A", "file": "../a.html" } ]""")))
        assertEquals(listOf(IssueCode.PAGE_FILE_PATH), codes(manifest("""[ { "id": "a", "title": "A", "file": "a.md" } ]""")))
        assertEquals(
            listOf(IssueCode.PAGE_FILE_REUSED),
            codes(manifest("""[ { "id": "a", "title": "A", "file": "a.html" }, { "id": "b", "title": "B", "file": "a.html" } ]""")),
        )
    }

    @Test
    fun treeDepthIsLimited() {
        var tree = """[ { "id": "leaf", "title": "L", "file": "a.html" } ]"""
        repeat(ThemeManifest.MAX_TREE_DEPTH) { level -> tree = """[ { "id": "s$level", "title": "S", "children": $tree } ]""" }

        assertEquals(listOf(IssueCode.TREE_TOO_DEEP), codes(manifest(tree)))
    }

    @Test
    fun unknownFieldsAreOnlyWarnings() {
        val tree = """[ { "id": "a", "title": "A", "file": "a.html", "icon": "x" } ]"""
        val result = ManifestParser.parse(manifest(tree, extra = """"author": "me","""))

        assertNotNull(result.manifest)
        assertEquals(listOf(IssueCode.MANIFEST_UNKNOWN_FIELD, IssueCode.MANIFEST_UNKNOWN_FIELD), result.issues.map { it.code })
        assertTrue(result.issues.all { it.severity == IssueSeverity.WARNING })
    }
}

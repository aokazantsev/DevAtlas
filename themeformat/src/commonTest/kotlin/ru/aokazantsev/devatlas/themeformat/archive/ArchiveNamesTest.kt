package ru.aokazantsev.devatlas.themeformat.archive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchiveNamesTest {

    @Test
    fun unsafeNamesAreRejected() {
        listOf("../a", "a/../../b", "/etc/x", "C:/x", "a\\b", "a/\u0001b", "").forEach { name ->
            assertNotNull(ArchiveEntryNames.unsafeReason(name), name)
        }
        listOf("a", "pages/a.html", "dir/", "a..b/c").forEach { name ->
            assertNull(ArchiveEntryNames.unsafeReason(name), name)
        }
    }

    @Test
    fun systemLeftoversAreIgnored() {
        assertTrue(ArchiveEntryNames.isIgnored("__MACOSX/theme/._a.html"))
        assertTrue(ArchiveEntryNames.isIgnored("theme/.DS_Store"))
        assertFalse(ArchiveEntryNames.isIgnored("theme/a.html"))
    }

    @Test
    fun rootIsArchiveRootOrSingleFolder() {
        assertEquals("", ThemeRootLocator.locate(listOf("manifest.json", "pages/a.html")))
        assertEquals("theme-main/", ThemeRootLocator.locate(listOf("theme-main/manifest.json", "theme-main/a.html")))
        assertNull(ThemeRootLocator.locate(listOf("a/manifest.json", "b/x.html")))
        assertNull(ThemeRootLocator.locate(listOf("readme.md", "a/manifest.json")))
        assertNull(ThemeRootLocator.locate(listOf("a/b/manifest.json")))
    }
}

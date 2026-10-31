package ru.aokazantsev.devatlas.files.domain

import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PdfFileNamesTest {

    private val day = 1_761_868_800_000L

    private fun choose(request: PdfRequest, disposition: String? = null, url: String = request.url) =
        PdfFileNames.choose(request, disposition, url, day)

    @Test
    fun downloadAttributeWinsOverEverything() {
        val request = PdfRequest(url = "https://example.com/a.pdf", fileName = "guide.pdf", title = "Страница")

        assertEquals("guide.pdf", choose(request, disposition = "attachment; filename=\"server.pdf\""))
    }

    @Test
    fun contentDispositionGoesBeforeUrlAndTitle() {
        val request = PdfRequest(url = "https://example.com/a.pdf", title = "Страница")

        val encoded = "attachment; filename*=UTF-8''%D0%9E%D1%82%D1%87%D1%91%D1%82%202026.pdf"
        assertEquals("Отчёт 2026.pdf", choose(request, disposition = encoded))
        assertEquals("server.pdf", choose(request, disposition = "attachment; filename=\"server.pdf\""))
        assertEquals("plain.pdf", choose(request, disposition = "inline; filename=plain.pdf"))
    }

    @Test
    fun webViewDispositionIsUsedWhenResponseHasNone() {
        val request = PdfRequest(url = "https://example.com/get?id=1", contentDisposition = "attachment; filename=\"from-webview.pdf\"")

        assertEquals("from-webview.pdf", choose(request))
    }

    @Test
    fun urlFileNameGoesBeforeTitle() {
        val request = PdfRequest(url = "https://example.com/docs/rfc%209110.pdf?x=1", title = "Страница")

        assertEquals("rfc 9110.pdf", choose(request))
    }

    @Test
    fun titleIsUsedWhenUrlHasNoPdfName() {
        val request = PdfRequest(url = "https://example.com/get?id=1", title = "Отчёт: итоги / год")

        assertEquals("Отчёт итоги год.pdf", choose(request))
    }

    @Test
    fun domainAndDateAreTheLastResort() {
        assertEquals("files.example.com 2025-10-31.pdf", choose(PdfRequest(url = "https://files.example.com/get?id=1")))
    }

    @Test
    fun sanitizeDropsFoldersForbiddenCharsAndAddsExtension() {
        assertEquals("evil.pdf", PdfFileNames.sanitize("../../evil.pdf"))
        assertEquals("a b c.pdf", PdfFileNames.sanitize("a<b>c"))
        assertEquals("invoice exe.pdf", PdfFileNames.sanitize("invoice‮exe"))
        assertEquals("doc.pdf", PdfFileNames.sanitize("doc.PDF"))
        assertNull(PdfFileNames.sanitize(" ... "))
        assertEquals(124, PdfFileNames.sanitize("x".repeat(500))?.length)
    }

    @Test
    fun brokenPercentEncodingFallsBackToPlainName() {
        assertEquals("fallback.pdf", PdfFileNames.fromDisposition("attachment; filename*=UTF-8''%FF%FE; filename=\"fallback.pdf\""))
    }

    @Test
    fun pdfUrlIsDetectedByPathOnly() {
        assertTrue(PdfFileNames.isPdfUrl("https://example.com/a.PDF#page=2"))
        assertTrue(PdfFileNames.isPdfUrl("https://example.com/a.pdf?download=1"))
        assertFalse(PdfFileNames.isPdfUrl("https://example.com/get?file=a.pdf"))
    }
}

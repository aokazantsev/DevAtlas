package ru.aokazantsev.devatlas.ui.page

import ru.aokazantsev.devatlas.files.domain.PdfFileNames
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.ui.links.LinkActions
import ru.aokazantsev.devatlas.ui.page.content.externalUri

internal fun LinkActions.follow(link: PageInline.Link, pageTitle: String) {
    when (val target = link.target) {
        is LinkTarget.Page -> openPage(target.id)
        is LinkTarget.Web -> {
            val pdf = link.pdfRequest(pageTitle)
            if (pdf != null) openPdf(pdf) else openWeb(target.url)
        }
        is LinkTarget.Email -> openExternal(target.uri)
        is LinkTarget.Phone -> openExternal(target.uri)
    }
}

internal fun PageInline.Link.pdfRequest(pageTitle: String): PdfRequest? {
    val url = (target as? LinkTarget.Web)?.url ?: return null
    if (!PdfFileNames.isPdfUrl(url)) return null
    return PdfRequest(url = url, fileName = download, title = pageTitle)
}

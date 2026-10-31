package ru.aokazantsev.devatlas.ui.page

import androidx.compose.runtime.saveable.Saver
import ru.aokazantsev.devatlas.files.domain.PdfFileNames
import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.ui.page.content.externalUri

internal class LinkMenuModel(val uri: String, val pdf: Boolean, val fileName: String?) {

    fun pdfRequest(pageTitle: String): PdfRequest? = if (pdf) PdfRequest(url = uri, fileName = fileName, title = pageTitle) else null

    companion object {
        val NullableSaver: Saver<LinkMenuModel?, Any> = Saver(
            save = { model -> model?.let { arrayListOf(it.uri, it.pdf, it.fileName) } },
            restore = { saved ->
                val fields = saved as List<*>
                LinkMenuModel(fields[0] as String, fields[1] as Boolean, fields[2] as String?)
            },
        )
    }
}

internal fun PageInline.Link.menuModel(): LinkMenuModel? {
    val uri = externalUri() ?: return null
    val pdf = target is LinkTarget.Web && PdfFileNames.isPdfUrl(uri)
    return LinkMenuModel(uri = uri, pdf = pdf, fileName = download)
}

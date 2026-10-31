package ru.aokazantsev.devatlas.ui.page.content

import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline

internal fun PageInline.Link.externalUri(): String? = when (val target = target) {
    is LinkTarget.Page -> null
    is LinkTarget.Web -> target.url
    is LinkTarget.Email -> target.uri
    is LinkTarget.Phone -> target.uri
}

internal fun PageInline.Link.plainText(): String = buildString {
    content.forEach { inline ->
        when (inline) {
            is PageInline.Text -> append(inline.text)
            is PageInline.Link -> append(inline.plainText())
            PageInline.LineBreak -> append(' ')
        }
    }
}

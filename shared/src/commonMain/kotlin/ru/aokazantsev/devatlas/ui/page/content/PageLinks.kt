package ru.aokazantsev.devatlas.ui.page.content

import ru.aokazantsev.devatlas.themeformat.page.model.PageInline

internal class PageLinks(
    val onClick: (PageInline.Link) -> Unit,
    val onLongPress: (PageInline.Link) -> Unit,
)

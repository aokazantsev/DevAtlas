package ru.aokazantsev.devatlas.contents.domain.model

import ru.aokazantsev.devatlas.themeformat.page.model.PageDocument

data class PageContent(val document: PageDocument, val diagrams: Map<String, String>)

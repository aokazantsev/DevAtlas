package ru.aokazantsev.devatlas.contents.domain

import ru.aokazantsev.devatlas.contents.domain.model.PageContent
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

interface PageRepository {
    suspend fun load(themeId: String, page: ThemeNode.Page): PageContent?
}

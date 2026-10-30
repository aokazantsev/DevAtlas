package ru.aokazantsev.devatlas.contents

import ru.aokazantsev.devatlas.contents.domain.PageRepository
import ru.aokazantsev.devatlas.contents.domain.model.PageContent
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

class FakePageRepository : PageRepository {
    val contents = mutableMapOf<String, PageContent>()
    var loads = 0

    override suspend fun load(themeId: String, page: ThemeNode.Page): PageContent? {
        loads++
        return contents[page.file]
    }
}

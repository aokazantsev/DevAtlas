package ru.aokazantsev.devatlas.ui.themes

import ru.aokazantsev.devatlas.themes.domain.model.UpdateSummary
import ru.aokazantsev.devatlas.themes.preparedOf
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateSummaryTextTest {

    private fun available(summary: UpdateSummary, contentChanged: Boolean = true, sourceChanged: Boolean = false) =
        UpdateState.Available(preparedOf("a", "new", "p1"), summary, sourceChanged, contentChanged)

    @Test
    fun removedPagesWarnAboutMarksAndBookmarks() {
        val text = updateSummaryText(available(UpdateSummary(added = 1, removed = 1, changed = 1)))

        assertEquals(
            "Страниц добавлено: 1, удалено: 1, изменено: 1. " +
                "Отметки «Просмотрено» и закладки удалённых страниц и разделов сбросятся.",
            text,
        )
    }

    @Test
    fun removedSectionsAreNamedEvenWhenPagesAreUnchanged() {
        val text = updateSummaryText(available(UpdateSummary(added = 0, removed = 0, changed = 0, removedSections = 2)))

        assertEquals(
            "Страниц добавлено: 0, удалено: 0, изменено: 0. Разделов удалено: 2. " +
                "Отметки «Просмотрено» и закладки удалённых страниц и разделов сбросятся.",
            text,
        )
    }

    @Test
    fun addedOrChangedPagesDoNotWarnAboutRemoval() {
        val text = updateSummaryText(available(UpdateSummary(added = 1, removed = 0, changed = 2)))

        assertEquals("Страниц добавлено: 1, удалено: 0, изменено: 2.", text)
    }

    @Test
    fun unchangedPagesAndSections() {
        assertEquals(
            "Страницы не изменились: обновились схемы или другие файлы темы.",
            updateSummaryText(available(UpdateSummary(0, 0, 0))),
        )
        assertEquals(
            "Ссылка источника будет заменена. Содержимое не изменилось.",
            updateSummaryText(available(UpdateSummary(0, 0, 0), contentChanged = false, sourceChanged = true)),
        )
    }
}

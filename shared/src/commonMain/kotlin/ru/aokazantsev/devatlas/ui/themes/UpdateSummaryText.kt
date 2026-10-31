package ru.aokazantsev.devatlas.ui.themes

internal fun updateSummaryText(state: UpdateState.Available): String {
    val summary = state.summary
    val source = if (state.sourceChanged) "Ссылка источника будет заменена. " else ""
    val changes = when {
        summary.isEmpty && !state.contentChanged -> "Содержимое не изменилось."
        summary.isEmpty -> "Страницы не изменились: обновились схемы или другие файлы темы."
        else -> pagesChangeText(summary.added, summary.removed, summary.changed) + removalText(summary.removed, summary.removedSections)
    }
    return source + changes
}

private fun pagesChangeText(added: Int, removed: Int, changed: Int): String =
    "Страниц добавлено: $added, удалено: $removed, изменено: $changed."

private fun removalText(removedPages: Int, removedSections: Int): String {
    if (removedPages == 0 && removedSections == 0) return ""
    val sections = if (removedSections > 0) " Разделов удалено: $removedSections." else ""
    return "$sections Отметки «Просмотрено» и закладки удалённых страниц и разделов сбросятся."
}

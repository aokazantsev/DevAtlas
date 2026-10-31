package ru.aokazantsev.devatlas.usage.domain.model

enum class UsageEvent(val key: String) {
    APP_START("app.start"),
    SETTINGS_SAVE("settings.save"),
    UPDATE_CHECK("update.check"),
    UPDATE_DOWNLOAD("update.download"),
    UPDATE_INSTALL("update.install"),
    THEME_DOWNLOAD("theme.download"),
    THEME_UPDATE("theme.update"),
    THEME_DELETE("theme.delete"),
    THEME_SWITCH("theme.switch"),
    PAGE_MARK_SEEN("page.mark_seen"),
    PROGRESS_RESET("progress.reset"),
    DIAGRAM_FULLSCREEN("diagram.fullscreen"),
    LINK_WEBVIEW("link.webview"),
    PDF_OPEN("pdf.open"),
    PDF_DOWNLOAD("pdf.download"),
}

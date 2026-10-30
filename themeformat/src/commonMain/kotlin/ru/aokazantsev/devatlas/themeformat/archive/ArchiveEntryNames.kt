package ru.aokazantsev.devatlas.themeformat.archive

internal object ArchiveEntryNames {

    private val drive = Regex("^[A-Za-z]:")
    private val controlChars = Regex("[\\u0000-\\u001F]")

    fun unsafeReason(name: String): String? = when {
        name.isEmpty() -> "пустое имя"
        '\\' in name -> "обратный слеш в имени — архив собран не по стандарту zip"
        name.startsWith("/") || drive.containsMatchIn(name) -> "абсолютный путь"
        controlChars.containsMatchIn(name) -> "управляющие символы в имени"
        name.trimEnd('/').split('/').any { it == ".." } -> "выход из папки через .."
        name.trimEnd('/').split('/').any { it.isEmpty() } -> "пустой сегмент пути (//)"
        else -> null
    }

    fun withoutDotSegments(name: String): String = name.split('/').filterNot { it == "." }.joinToString("/")

    fun isIgnored(name: String): Boolean {
        val segments = name.trimEnd('/').split('/')
        return segments.first() == MAC_RESOURCE_FOLDER || segments.any { it.startsWith(".") && it != "." }
    }

    private const val MAC_RESOURCE_FOLDER = "__MACOSX"
}

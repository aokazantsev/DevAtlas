package ru.aokazantsev.devatlas.themeformat.path

object ThemePaths {

    private val forbiddenChars = Regex("[\\\\:*?\"<>|\\u0000-\\u001F]")

    private fun endsWithDotOrSpace(segment: String) = segment != "." && segment != ".." && (segment.endsWith(".") || segment.endsWith(" "))

    fun resolve(baseFile: String, reference: String): String? {
        if (reference.isBlank() || reference.startsWith("/") || forbiddenChars.containsMatchIn(reference)) return null
        if (reference.split('/').any(::endsWithDotOrSpace)) return null
        val segments = baseFile.split('/').dropLast(1).toMutableList()
        for (segment in reference.split('/')) {
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isEmpty()) return null else segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        return segments.joinToString("/").takeIf { it.isNotEmpty() }
    }

    fun isPlainRelative(path: String): Boolean =
        path.isNotBlank() &&
            !forbiddenChars.containsMatchIn(path) &&
            path.split('/').none { it.isEmpty() || it == "." || it == ".." || endsWithDotOrSpace(it) }

    fun extension(path: String): String = path.substringAfterLast('/').substringAfterLast('.', "").lowercase()
}

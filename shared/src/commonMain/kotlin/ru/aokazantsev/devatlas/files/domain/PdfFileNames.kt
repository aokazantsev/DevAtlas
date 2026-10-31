package ru.aokazantsev.devatlas.files.domain

import ru.aokazantsev.devatlas.files.domain.model.PdfRequest
import ru.aokazantsev.devatlas.text.CivilDate

object PdfFileNames {

    private const val EXTENSION = ".pdf"
    private const val MAX_BASE_LENGTH = 120
    private const val HEX_RADIX = 16
    private const val PERCENT_LENGTH = 3
    private val FORBIDDEN = Regex("[\\\\/:*?\"<>|\\p{Cntrl}\\p{Cf}\\u0080-\\u009F]")
    private val SPACES = Regex("\\s+")
    private val EXTENDED_NAME = Regex("filename\\*\\s*=\\s*([^']*)'[^']*'([^;]+)", RegexOption.IGNORE_CASE)
    private val QUOTED_NAME = Regex("filename\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", RegexOption.IGNORE_CASE)
    private val PLAIN_NAME = Regex("filename\\s*=\\s*([^;\\s]+)", RegexOption.IGNORE_CASE)

    fun isPdfUrl(url: String): Boolean = url.substringBefore('#').substringBefore('?').endsWith(EXTENSION, ignoreCase = true)

    fun choose(request: PdfRequest, responseDisposition: String?, finalUrl: String, localMillis: Long): String =
        sequenceOf(
            { request.fileName },
            { fromDisposition(request.contentDisposition ?: responseDisposition) },
            { fromUrl(finalUrl) },
            { request.title },
        ).firstNotNullOfOrNull { candidate -> candidate()?.let(::sanitize) }
            ?: fallback(finalUrl, localMillis)

    fun sanitize(raw: String): String? {
        val cleaned = raw.replace(FORBIDDEN, " ").replace(SPACES, " ").trim().trim('.', ' ')
        val base = if (cleaned.endsWith(EXTENSION, ignoreCase = true)) cleaned.dropLast(EXTENSION.length) else cleaned
        val trimmed = base.trim().trimEnd('.', ' ').take(MAX_BASE_LENGTH).trimEnd()
        return if (trimmed.isEmpty()) null else trimmed + EXTENSION
    }

    fun fromDisposition(header: String?): String? {
        if (header.isNullOrBlank()) return null
        EXTENDED_NAME.find(header)?.let { match ->
            val charset = match.groupValues[1].trim()
            if (charset.equals("UTF-8", ignoreCase = true)) percentDecode(match.groupValues[2].trim())?.let { return it }
        }
        QUOTED_NAME.find(header)?.let { return it.groupValues[1].replace("\\\"", "\"").replace("\\\\", "\\") }
        return PLAIN_NAME.find(header)?.groupValues?.get(1)
    }

    private fun fromUrl(url: String): String? {
        val path = url.substringBefore('#').substringBefore('?').substringAfter("://").substringAfter('/', "")
        val segment = path.substringAfterLast('/')
        if (!segment.endsWith(EXTENSION, ignoreCase = true)) return null
        return percentDecode(segment) ?: segment
    }

    private fun fallback(url: String, localMillis: Long): String {
        val host = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':')
        val date = CivilDate.ofEpochMillis(localMillis).iso()
        return sanitize(if (host.isEmpty()) "document $date" else "$host $date") ?: "document $date$EXTENSION"
    }

    private fun percentDecode(value: String): String? {
        val bytes = ArrayList<Byte>(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '%') {
                if (index + PERCENT_LENGTH > value.length) return null
                val byte = value.substring(index + 1, index + PERCENT_LENGTH).toIntOrNull(HEX_RADIX) ?: return null
                bytes += byte.toByte()
                index += PERCENT_LENGTH
            } else {
                char.toString().encodeToByteArray().forEach { bytes += it }
                index++
            }
        }
        return try {
            bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }
    }
}

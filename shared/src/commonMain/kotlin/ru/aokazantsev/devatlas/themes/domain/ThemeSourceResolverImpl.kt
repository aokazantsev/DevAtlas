package ru.aokazantsev.devatlas.themes.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import okio.Buffer
import ru.aokazantsev.devatlas.di.NetworkEndpoints
import ru.aokazantsev.devatlas.themes.domain.model.ArchiveLocation
import ru.aokazantsev.devatlas.themes.domain.model.SourceResolution
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan

@ContributesBinding(AppScope::class)
@Inject
internal class ThemeSourceResolverImpl(
    private val endpoints: NetworkEndpoints,
) : ThemeSourceResolver {

    override fun resolve(input: String): SourceResolution {
        val text = singleLinkIn(input.trim())
        val rejection = rejectionFor(text)
        if (rejection != null) return SourceResolution.Rejected(rejection)
        val match = ADDRESS.matchEntire(text) ?: return SourceResolution.Rejected(NOT_AN_ADDRESS)
        val authority = match.groupValues[HOST_GROUP]
        val host = authority.lowercase().substringBefore(':')
        val segments = match.groupValues[PATH_GROUP].split('/').filter { it.isNotEmpty() }
        val address = text.substringBefore('#')
        return when {
            '@' in authority -> SourceResolution.Rejected(NOT_AN_ADDRESS)
            host in YANDEX_HOSTS -> yandex(address, host, segments)
            host in GOOGLE_HOSTS -> SourceResolution.Rejected(GOOGLE_DRIVE)
            host in DROPBOX_HOSTS -> SourceResolution.Rejected(DROPBOX)
            host in GITHUB_HOSTS && segments.getOrNull(MODE_INDEX) == "blob" && isZip(segments) -> githubFile(address, segments)
            isZip(segments) -> zipUrl(address)
            host in GITHUB_HOSTS -> github(address, segments)
            else -> SourceResolution.Rejected(UNKNOWN_SOURCE)
        }
    }

    private fun rejectionFor(text: String): String? {
        val scheme = text.substringBefore("://", missingDelimiterValue = "").lowercase()
        return when {
            text.isEmpty() -> "Вставьте ссылку на тему"
            text.any { it.isWhitespace() } -> "В тексте не только ссылка: скопируйте одну ссылку без текста вокруг"
            "://" !in text -> NOT_AN_ADDRESS
            scheme == "https" -> null
            scheme == "http" && endpoints.allowCleartext -> null
            scheme == "http" -> "Ссылки http:// не поддерживаются: нужен безопасный адрес https://"
            else -> "Адрес должен начинаться с https://"
        }
    }

    private fun singleLinkIn(text: String): String {
        if (text.none { it.isWhitespace() }) return text
        val links = LINK_IN_TEXT.findAll(text).map { it.value.trimEnd { char -> char in LINK_TRAILING_PUNCTUATION } }.toList()
        return links.singleOrNull() ?: text
    }

    private fun yandex(address: String, host: String, segments: List<String>): SourceResolution {
        val kind = segments.firstOrNull()
        val key = segments.getOrNull(1)
        if ((kind != "d" && kind != "i") || key == null) return SourceResolution.Rejected(YANDEX_NOT_PUBLIC)
        val folder = decodedFolder(segments.drop(KEY_INDEX + 1)) ?: return SourceResolution.Rejected(NOT_AN_ADDRESS)
        val path = folder.takeIf { it.isNotEmpty() }?.joinToString(separator = "/", prefix = "/")
        val plan = ThemeSourcePlan(
            address = address,
            location = ArchiveLocation.YandexPublic(publicKey = "https://$host/$kind/$key", path = path),
            subFolder = null,
        )
        return SourceResolution.Resolved(plan)
    }

    private fun zipUrl(address: String): SourceResolution {
        val plan = ThemeSourcePlan(
            address = address,
            location = ArchiveLocation.Direct(address),
            subFolder = null,
        )
        return SourceResolution.Resolved(plan)
    }

    private fun isZip(segments: List<String>) = segments.lastOrNull()?.endsWith(ZIP_SUFFIX, ignoreCase = true) == true

    private fun githubFile(address: String, segments: List<String>): SourceResolution {
        val rawPath = segments.mapIndexed { index, segment -> if (index == MODE_INDEX) "raw" else segment }.joinToString("/")
        val plan = ThemeSourcePlan(
            address = address,
            location = ArchiveLocation.Direct("https://github.com/$rawPath"),
            subFolder = null,
        )
        return SourceResolution.Resolved(plan)
    }

    private fun github(address: String, segments: List<String>): SourceResolution {
        val owner = segments.getOrNull(0)
        val repo = segments.getOrNull(1)?.removeSuffix(".git")
        val mode = segments.getOrNull(MODE_INDEX)
        val branch = segments.getOrNull(BRANCH_INDEX)
        val folder = decodedFolder(segments.drop(BRANCH_INDEX + 1))
        val wrongShape = owner == null || repo.isNullOrEmpty() || (mode != null && (mode != "tree" || branch == null))
        if (wrongShape || folder == null || branch == "..") return SourceResolution.Rejected(GITHUB_SHAPE)
        val plan = ThemeSourcePlan(
            address = address,
            location = ArchiveLocation.Direct("${endpoints.githubArchiveUrl.trimEnd('/')}/$owner/$repo/zip/${branch ?: "HEAD"}"),
            subFolder = folder.takeIf { it.isNotEmpty() }?.joinToString("/"),
        )
        return SourceResolution.Resolved(plan)
    }

    private fun decodedFolder(segments: List<String>): List<String>? {
        val decoded = segments.map { percentDecoded(it) ?: return null }
        return decoded.takeIf { folder -> folder.none { it == ".." || it == "." || '/' in it || '\\' in it } }
    }

    private fun percentDecoded(segment: String): String? {
        val buffer = Buffer()
        var index = 0
        while (index < segment.length) {
            val percent = segment.indexOf('%', startIndex = index).takeIf { it >= 0 } ?: segment.length
            buffer.writeUtf8(segment, index, percent)
            if (percent == segment.length) break
            val hex = segment.substring(percent + 1, minOf(percent + PERCENT_ESCAPE_LENGTH, segment.length))
            if (hex.length != 2 || hex.any { it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' }) return null
            buffer.writeByte(hex.toInt(HEX_RADIX))
            index = percent + PERCENT_ESCAPE_LENGTH
        }
        return buffer.readUtf8().takeIf { '�' !in it }
    }

    private companion object {
        val ADDRESS = Regex("^[A-Za-z][A-Za-z0-9+.-]*://([^/?#]*)([^?#]*)(\\?[^#]*)?(#.*)?$")
        val LINK_IN_TEXT = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        const val LINK_TRAILING_PUNCTUATION = ".,;:!?)»\"'"
        const val HOST_GROUP = 1
        const val PATH_GROUP = 2
        const val KEY_INDEX = 1
        const val MODE_INDEX = 2
        const val BRANCH_INDEX = 3
        const val ZIP_SUFFIX = ".zip"
        const val PERCENT_ESCAPE_LENGTH = 3
        const val HEX_RADIX = 16
        val YANDEX_HOSTS = setOf("disk.yandex.ru", "disk.yandex.com", "disk.yandex.by", "disk.yandex.kz", "disk.yandex.uz", "yadi.sk")
        val GOOGLE_HOSTS = setOf("drive.google.com", "docs.google.com")
        val DROPBOX_HOSTS = setOf("dropbox.com", "www.dropbox.com")
        val GITHUB_HOSTS = setOf("github.com", "www.github.com")
        const val NOT_AN_ADDRESS = "Это не похоже на ссылку: нужен адрес вида https://…"
        const val GOOGLE_DRIVE = "Google Диск не поддерживается: выложите тему на Яндекс.Диск, GitHub или любой сайт в виде zip-архива"
        const val DROPBOX = "Dropbox пока не поддерживается: выложите тему на Яндекс.Диск, GitHub или любой сайт в виде zip-архива"
        const val YANDEX_NOT_PUBLIC = "Нужна публичная ссылка на папку или файл Яндекс.Диска: disk.yandex.ru/d/… или disk.yandex.ru/i/…"
        const val GITHUB_SHAPE =
            "Для GitHub нужна ссылка на репозиторий или его папку: github.com/владелец/репозиторий или …/tree/ветка/папка"
        const val UNKNOWN_SOURCE =
            "Не понимаю этот адрес. Подходят публичная папка или zip Яндекс.Диска, репозиторий GitHub и ссылка на файл .zip. " +
                "Обычные папки на сайтах не поддерживаются"
    }
}

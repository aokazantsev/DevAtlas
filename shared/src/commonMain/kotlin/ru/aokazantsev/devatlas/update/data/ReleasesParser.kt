package ru.aokazantsev.devatlas.update.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import ru.aokazantsev.devatlas.update.domain.model.AppRelease
import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import ru.aokazantsev.devatlas.update.domain.model.ReleaseApk
import ru.aokazantsev.devatlas.update.domain.model.ReleaseList

internal object ReleasesParser {

    const val PAGE_SIZE = 30
    private const val SHA256_PREFIX = "sha256:"
    private const val APK_EXTENSION = ".apk"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun parse(text: String): ReleaseList? {
        val array = try {
            Json.parseToJsonElement(text) as? JsonArray
        } catch (_: SerializationException) {
            null
        } ?: return null
        val releases = array.mapNotNull { (it as? JsonObject)?.let(::release) }
        return ReleaseList(releases = releases, complete = array.size < PAGE_SIZE)
    }

    private fun release(item: JsonObject): AppRelease? {
        if (item.flag("draft") || item.flag("prerelease")) return null
        val version = item.text("tag_name")?.let(AppVersion::parse) ?: return null
        val assets = (item["assets"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val apk = assets.firstOrNull { it.text("name")?.endsWith(APK_EXTENSION, ignoreCase = true) == true }
        return AppRelease(
            version = version,
            bodyHtml = item.text("body_html").orEmpty(),
            apk = apk?.let(::apkOf),
        )
    }

    private fun apkOf(asset: JsonObject): ReleaseApk? {
        val url = asset.text("browser_download_url") ?: return null
        val digest = asset.text("digest")?.lowercase()
        return ReleaseApk(
            url = url,
            sizeBytes = asset.number("size") ?: 0L,
            sha256 = digest?.removePrefix(SHA256_PREFIX)?.takeIf { digest.startsWith(SHA256_PREFIX) && SHA256.matches(it) },
        )
    }

    private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)
        ?.takeIf { it.isString }
        ?.contentOrNull

    private fun JsonObject.flag(name: String): Boolean = runCatching { this[name]?.jsonPrimitive?.booleanOrNull }.getOrNull() == true

    private fun JsonObject.number(name: String): Long? = runCatching { this[name]?.jsonPrimitive?.longOrNull }.getOrNull()
}

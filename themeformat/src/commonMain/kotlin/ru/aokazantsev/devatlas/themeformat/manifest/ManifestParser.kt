package ru.aokazantsev.devatlas.themeformat.manifest

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueCollector
import ru.aokazantsev.devatlas.themeformat.markup.SourcePosition

object ManifestParser {

    private val offsetPattern = Regex("offset (\\d+)")

    fun parse(source: String): ManifestParseResult {
        val collector = IssueCollector(ThemeManifest.FILE_NAME)
        val root = try {
            Json.parseToJsonElement(source)
        } catch (error: SerializationException) {
            collector.error(IssueCode.MANIFEST_SYNTAX, "manifest.json — не корректный JSON", syntaxPosition(source, error))
            return ManifestParseResult(null, collector.issues)
        }
        if (root !is JsonObject) {
            collector.error(IssueCode.MANIFEST_SYNTAX, "manifest.json должен быть объектом { … }")
            return ManifestParseResult(null, collector.issues)
        }
        return ManifestParseResult(ManifestReader(collector).read(root), collector.issues)
    }

    private fun syntaxPosition(source: String, error: SerializationException): SourcePosition? {
        val offset = error.message?.let { offsetPattern.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val before = source.take(offset)
        return SourcePosition(line = before.count { it == '\n' } + 1, column = offset - before.lastIndexOf('\n'))
    }
}

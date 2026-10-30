package ru.aokazantsev.devatlas.themeformat.manifest

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueCollector
import ru.aokazantsev.devatlas.themeformat.path.ThemePaths

internal class ManifestReader(private val collector: IssueCollector) {

    private val rootKeys = setOf("formatVersion", "id", "title", "description", "tree")
    private val nodeKeys = setOf("id", "title", "file", "children")
    private val pageExtensions = setOf("html", "htm")
    private val seenIds = mutableSetOf<String>()
    private val seenFiles = mutableSetOf<String>()

    fun read(root: JsonObject): ThemeManifest? {
        reportUnknownKeys(root, rootKeys, "")
        val formatVersion = root["formatVersion"]?.let { it as? JsonPrimitive }?.takeUnless { it.isString }?.intOrNull
        when {
            formatVersion == null -> field("formatVersion", "нужно число — версия формата, сейчас ${ThemeManifest.CURRENT_FORMAT_VERSION}")
            formatVersion != ThemeManifest.CURRENT_FORMAT_VERSION -> collector.error(
                IssueCode.FORMAT_VERSION,
                "formatVersion = $formatVersion: приложение понимает формат ${ThemeManifest.CURRENT_FORMAT_VERSION}",
            )
        }
        val id = string(root, "id", "")
        if (id != null && !ThemeIds.isValid(id)) invalidId("id", id)
        val title = string(root, "title", "")?.takeIf { it.isNotBlank() }
        if (root.containsKey("title") && title == null) field("title", "название темы не может быть пустым")
        val description = optionalString(root, "description", "")
        val tree = nodes(root["tree"], "tree", depth = 1)
        val names = if (id != null && title != null) id to title else null
        return if (formatVersion == null || tree == null || names == null) {
            null
        } else {
            ThemeManifest(formatVersion, names.first, names.second, description, tree)
        }
    }

    private fun nodes(element: JsonElement?, path: String, depth: Int): List<ThemeNode>? {
        if (element !is JsonArray || element.isEmpty()) {
            field(path, "нужен непустой список узлов [ … ]")
            return null
        }
        if (depth > ThemeManifest.MAX_TREE_DEPTH) {
            collector.error(IssueCode.TREE_TOO_DEEP, "$path: дерево глубже ${ThemeManifest.MAX_TREE_DEPTH} уровней")
            return null
        }
        return element.mapIndexedNotNull { index, child -> node(child, "$path[$index]", depth) }.takeIf { it.isNotEmpty() }
    }

    private fun node(element: JsonElement, path: String, depth: Int): ThemeNode? {
        if (element !is JsonObject) {
            field(path, "узел должен быть объектом { \"id\": …, \"title\": … }")
            return null
        }
        reportUnknownKeys(element, nodeKeys, path)
        val id = string(element, "id", path)?.also { checkNodeId(it, "$path.id") }
        val title = string(element, "title", path)?.takeIf { it.isNotBlank() }
        if (element.containsKey("title") && title == null) field("$path.title", "название не может быть пустым")
        val hasFile = element.containsKey("file")
        val hasChildren = element.containsKey("children")
        if (hasFile == hasChildren) {
            collector.error(
                IssueCode.NODE_SHAPE,
                "$path: у узла должно быть ровно одно из полей — file (страница) или children (раздел)",
            )
            return null
        }
        return if (hasFile) {
            val file = string(element, "file", path)?.also { checkPageFile(it, "$path.file") }
            if (id == null || title == null || file == null) null else ThemeNode.Page(id, title, file)
        } else {
            val children = nodes(element["children"], "$path.children", depth + 1)
            if (id == null || title == null || children == null) null else ThemeNode.Section(id, title, children)
        }
    }

    private fun checkNodeId(id: String, path: String) {
        if (!ThemeIds.isValid(id)) {
            invalidId(path, id)
        } else if (!seenIds.add(id)) {
            collector.error(IssueCode.ID_DUPLICATE, "$path: id «$id» уже занят другим узлом — id должны быть уникальными")
        }
    }

    private fun checkPageFile(file: String, path: String) {
        when {
            !ThemePaths.isPlainRelative(file) -> collector.error(
                IssueCode.PAGE_FILE_PATH,
                "$path: «$file» — нужен путь внутри папки темы через /, без .. и без / в начале",
            )
            ThemePaths.extension(file) !in pageExtensions ->
                collector.error(IssueCode.PAGE_FILE_PATH, "$path: «$file» — страница должна быть файлом .html")
            !seenFiles.add(file) ->
                collector.warning(IssueCode.PAGE_FILE_REUSED, "$path: файл «$file» уже используется другой страницей")
        }
    }

    private fun invalidId(path: String, id: String) {
        collector.error(
            IssueCode.ID_INVALID,
            "${path.ifEmpty { "id" }}: «$id» — id из строчных латинских букв, цифр и дефисов, до ${ThemeIds.MAX_LENGTH} символов",
        )
    }

    private fun string(element: JsonObject, key: String, path: String): String? {
        val value = element[key]
        val content = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (content == null) field(join(path, key), if (value == null) "поле обязательно" else "нужна строка в кавычках")
        return content
    }

    private fun optionalString(element: JsonObject, key: String, path: String): String? {
        val value = element[key] ?: return null
        val content = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (content == null) field(join(path, key), "нужна строка в кавычках")
        return content?.takeIf { it.isNotBlank() }
    }

    private fun reportUnknownKeys(element: JsonObject, known: Set<String>, path: String) {
        (element.keys - known).forEach { key ->
            collector.warning(IssueCode.MANIFEST_UNKNOWN_FIELD, "${join(path, key)}: неизвестное поле, приложение его пропустит")
        }
    }

    private fun field(path: String, problem: String) {
        collector.error(IssueCode.MANIFEST_FIELD, "$path: $problem")
    }

    private fun join(path: String, key: String) = if (path.isEmpty()) key else "$path.$key"
}

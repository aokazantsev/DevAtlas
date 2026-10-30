package ru.aokazantsev.devatlas.themeformat.svg

import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueCollector
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.markup.MarkupAttribute
import ru.aokazantsev.devatlas.themeformat.markup.MarkupDialect
import ru.aokazantsev.devatlas.themeformat.markup.MarkupNode
import ru.aokazantsev.devatlas.themeformat.markup.MarkupProblemKind

object SvgChecker {

    private val colorProperties = setOf("fill", "stroke", "stop-color", "flood-color", "lighting-color", "color")
    private val paintProperties = setOf("fill", "stroke")
    private val keywords = setOf("none", "transparent", "currentcolor", "inherit")
    private val animationElements = setOf("set", "animate", "animateTransform", "animateMotion", "animateColor", "discard")
    private val forbiddenElements = setOf("script", "foreignObject", "style", "image", "iframe", "audio", "video") + animationElements
    private const val HREF = "href"
    private val variable = Regex("var\\(\\s*--([A-Za-z0-9-]+)\\s*\\)")
    private val localUrl = Regex("url\\(\\s*['\"]?#[^)'\"\\s]+['\"]?\\s*\\)")
    private val urlReference = Regex("url\\(\\s*['\"]?([^)'\"]*)", RegexOption.IGNORE_CASE)

    fun check(file: String, source: String): List<ThemeIssue> {
        val collector = IssueCollector(file)
        val (nodes, problems) = MarkupDialect.Xml.parse(source)
        problems.forEach { problem ->
            val message = when (problem.kind) {
                MarkupProblemKind.SYNTAX, MarkupProblemKind.ENTITY -> problem.message
                MarkupProblemKind.STRUCTURE -> "${problem.message}: SVG должен быть корректным XML"
            }
            collector.error(IssueCode.SVG_SYNTAX, message, problem.position)
        }
        val elements = nodes.filterIsInstance<MarkupNode.Element>()
        val root = elements.singleOrNull()
        if (root == null || localName(root.name) != "svg") {
            collector.error(IssueCode.SVG_SYNTAX, "В файле должен быть ровно один корневой элемент <svg>")
            return collector.issues
        }
        if (root.attribute("viewBox") == null) {
            collector.warning(
                IssueCode.SVG_NO_VIEWBOX,
                "У <svg> нет viewBox: схема не будет масштабироваться под ширину экрана",
                root.position,
            )
        }
        if (root.attribute("fill") == null && root.attribute("style")?.value?.contains("fill") != true) {
            collector.warning(
                IssueCode.SVG_NO_ROOT_FILL,
                "У <svg> не задан fill: фигуры без своего цвета станут чёрными, задайте fill=\"currentColor\"",
                root.position,
            )
        }
        checkElement(root, collector)
        return collector.issues
    }

    private fun checkElement(element: MarkupNode.Element, collector: IssueCollector) {
        val name = localName(element.name)
        if (name in forbiddenElements) {
            collector.error(IssueCode.SVG_FORBIDDEN, forbiddenMessage(name), element.position)
            return
        }
        element.attributes.forEach { checkAttribute(it, collector) }
        element.children.filterIsInstance<MarkupNode.Element>().forEach { checkElement(it, collector) }
    }

    private fun checkAttribute(attribute: MarkupAttribute, collector: IssueCollector) {
        val name = attribute.name
        val value = attribute.value.orEmpty()
        when {
            name.lowercase().startsWith("on") ->
                collector.error(IssueCode.SVG_FORBIDDEN, "Атрибут $name запрещён: скрипты в схемах не выполняются", attribute.position)
            localName(name) == HREF && !value.trim().startsWith("#") ->
                collector.error(
                    IssueCode.SVG_EXTERNAL_REFERENCE,
                    "$name=\"$value\": схема может ссылаться только на свои элементы (#id)",
                    attribute.position,
                )
            name == "style" -> checkStyle(value, attribute, collector)
            name in colorProperties -> checkColor(name, value, attribute, collector)
            hasExternalUrl(value) ->
                collector.error(
                    IssueCode.SVG_EXTERNAL_REFERENCE,
                    "$name=\"$value\": url() может ссылаться только на #id",
                    attribute.position,
                )
        }
    }

    private fun checkStyle(style: String, attribute: MarkupAttribute, collector: IssueCollector) {
        style.split(';').forEach { declaration ->
            val property = declaration.substringBefore(':', "").trim().lowercase()
            val value = declaration.substringAfter(':', "").trim()
            when {
                property in colorProperties -> checkColor(property, value, attribute, collector)
                hasExternalUrl(value) -> collector.error(
                    IssueCode.SVG_EXTERNAL_REFERENCE,
                    "style: $property: $value — url() может ссылаться только на #id",
                    attribute.position,
                )
            }
        }
    }

    private fun hasExternalUrl(value: String): Boolean =
        urlReference.findAll(value).any { !it.groupValues[1].trim().startsWith("#") }

    private fun checkColor(property: String, rawValue: String, attribute: MarkupAttribute, collector: IssueCollector) {
        val value = rawValue.trim().removeSuffix("!important").trim()
        if (value.lowercase() in keywords) return
        if (property in paintProperties && localUrl.matches(value)) return
        val match = variable.matchEntire(value)
        if (match == null) {
            collector.error(
                IssueCode.COLOR_LITERAL,
                "$property=\"$rawValue\": цвет задаётся только токеном — currentColor или var(--роль), например var(--primary)",
                attribute.position,
            )
            return
        }
        val role = match.groupValues[1]
        if (ColorRole.fromCssName(role) == null) {
            collector.error(
                IssueCode.COLOR_ROLE_UNKNOWN,
                "Роли --$role нет в схеме цветов: используйте роли из списка, например --primary, --on-surface, --outline",
                attribute.position,
            )
        }
    }

    private fun forbiddenMessage(name: String) = when (name) {
        "script" -> "<script> запрещён: скрипты в схемах не выполняются"
        "style" -> "<style> не поддерживается: задавайте цвета атрибутами fill и stroke"
        "image" -> "<image> не поддерживается: картинки внутри схемы не показываются"
        in animationElements -> "<$name> запрещён: анимация в схемах не поддерживается, схема статична"
        else -> "<$name> не поддерживается в схемах"
    }

    private fun localName(name: String) = name.substringAfter(':')
}

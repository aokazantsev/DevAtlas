package ru.aokazantsev.devatlas.themeformat.page

import ru.aokazantsev.devatlas.themeformat.code.CodeHighlighter
import ru.aokazantsev.devatlas.themeformat.code.CodeLanguage
import ru.aokazantsev.devatlas.themeformat.code.JsonSyntax
import ru.aokazantsev.devatlas.themeformat.issue.IssueCode
import ru.aokazantsev.devatlas.themeformat.issue.IssueCollector
import ru.aokazantsev.devatlas.themeformat.issue.ThemeIssue
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeIds
import ru.aokazantsev.devatlas.themeformat.markup.MarkupNode
import ru.aokazantsev.devatlas.themeformat.markup.MarkupProblem
import ru.aokazantsev.devatlas.themeformat.markup.MarkupProblemKind
import ru.aokazantsev.devatlas.themeformat.page.model.InlineStyle
import ru.aokazantsev.devatlas.themeformat.page.model.LinkTarget
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.page.model.PageDocument
import ru.aokazantsev.devatlas.themeformat.page.model.PageInline
import ru.aokazantsev.devatlas.themeformat.page.model.TableCell
import ru.aokazantsev.devatlas.themeformat.page.model.TableRow
import ru.aokazantsev.devatlas.themeformat.path.ThemePaths

internal class HtmlPageBuilder(private val file: String) {

    private val collector = IssueCollector(file)
    private var linkDepth = 0
    val pageReferences = mutableListOf<Reference>()
    val imageReferences = mutableListOf<Reference>()

    val issues: List<ThemeIssue>
        get() = collector.issues

    fun markupProblem(problem: MarkupProblem) {
        when (problem.kind) {
            MarkupProblemKind.SYNTAX -> collector.error(IssueCode.MARKUP_SYNTAX, problem.message, problem.position)
            MarkupProblemKind.STRUCTURE -> collector.warning(IssueCode.TAG_STRUCTURE, problem.message, problem.position)
            MarkupProblemKind.ENTITY -> collector.warning(IssueCode.ENTITY_UNKNOWN, problem.message, problem.position)
        }
    }

    fun build(nodes: List<MarkupNode>): PageDocument = PageDocument(blocks(nodes))

    private fun blocks(nodes: List<MarkupNode>): List<PageBlock> {
        val result = mutableListOf<PageBlock>()
        val pending = mutableListOf<PageInline>()
        fun flush() {
            val content = normalize(pending)
            if (content.isNotEmpty()) result += PageBlock.Paragraph(content)
            pending.clear()
        }
        for (node in flatten(nodes)) {
            when {
                node is MarkupNode.Text -> pending += inline(node, InlineStyle.Plain)
                node !is MarkupNode.Element -> Unit
                node.name in HtmlTags.blocks -> {
                    flush()
                    result += block(node)
                }
                node.isInline() -> pending += inline(node, InlineStyle.Plain)
                else -> {
                    misplaced(node, "<${node.name}> стоит вне своего контейнера: содержимое показано как обычный текст")
                    flush()
                    result += blocks(node.children)
                }
            }
        }
        flush()
        return result
    }

    private fun block(element: MarkupNode.Element): List<PageBlock> {
        val level = HtmlTags.headings[element.name]
        return when {
            level != null -> listOfNotNull(
                normalize(inlines(element.children, InlineStyle.Plain))
                    .takeIf { it.isNotEmpty() }
                    ?.let { PageBlock.Heading(level, it) },
            )
            element.name == "p" -> paragraph(element)
            element.name == "ul" || element.name == "ol" -> listOf(list(element))
            element.name == "pre" -> listOf(code(element))
            element.name == "blockquote" -> listOf(PageBlock.Quote(blocks(element.children)))
            element.name == "table" -> listOfNotNull(table(element))
            element.name == "figure" -> figure(element)
            element.name == "hr" -> listOf(PageBlock.Divider)
            else -> listOfNotNull(image(element, caption = emptyList()))
        }
    }

    private fun paragraph(element: MarkupNode.Element): List<PageBlock> {
        val result = mutableListOf<PageBlock>()
        val pending = mutableListOf<PageInline>()
        fun flush() {
            val content = normalize(pending)
            if (content.isNotEmpty()) result += PageBlock.Paragraph(content)
            pending.clear()
        }
        for (node in flatten(element.children)) {
            if (node is MarkupNode.Element && node.name == "img") {
                flush()
                image(node, caption = emptyList())?.let { result += it }
            } else {
                pending += inline(node, InlineStyle.Plain)
            }
        }
        flush()
        return result
    }

    private fun list(element: MarkupNode.Element): PageBlock.ListBlock {
        val items = flatten(element.children).mapNotNull { child ->
            when {
                child is MarkupNode.Text && child.text.isBlank() -> null
                child is MarkupNode.Element && child.name == "li" -> blocks(child.children)
                else -> {
                    misplaced(child, "Внутри <${element.name}> ожидаются пункты <li>")
                    blocks(listOf(child))
                }
            }
        }
        return PageBlock.ListBlock(ordered = element.name == "ol", items = items)
    }

    private fun code(element: MarkupNode.Element): PageBlock.Code {
        val codeElement = element.children
            .filterNot { it is MarkupNode.Text && it.text.isBlank() }
            .singleOrNull()
            ?.let { it as? MarkupNode.Element }
            ?.takeIf { it.name == "code" }
        val language = codeElement?.attribute("class")?.value
            ?.split(' ')
            ?.firstOrNull { it.startsWith(LANGUAGE_PREFIX) }
            ?.removePrefix(LANGUAGE_PREFIX)
            ?.takeIf { it.isNotEmpty() }
        val container = codeElement ?: element
        firstForeignTag(container.children, allowed = emptySet())?.let(::codeMarkup)
        val text = container.textContent()
            .replace("\r\n", "\n")
            .removePrefix("\n")
            .trimEnd()
        checkLanguage(element, language, text)
        checkLength(element, language, text)
        return PageBlock.Code(code = text, language = language)
    }

    private fun checkLength(element: MarkupNode.Element, language: String?, text: String) {
        val resolved = language?.let(CodeLanguage::resolve) ?: return
        if (resolved == CodeLanguage.TEXT || text.length <= CodeHighlighter.MAX_CODE_LENGTH) return
        collector.warning(
            IssueCode.CODE_TOO_LONG,
            "Блок кода длиннее ${CodeHighlighter.MAX_CODE_LENGTH} символов показывается без подсветки: разбейте его на части",
            element.position,
        )
    }

    private fun checkLanguage(element: MarkupNode.Element, language: String?, text: String) {
        val resolved = language?.let(CodeLanguage::resolve)
        when {
            language == null -> collector.warning(
                IssueCode.CODE_LANGUAGE,
                "У блока кода не указан язык: блок показан без подсветки. Добавьте <code class=\"language-kotlin\">, " +
                    "для простого текста — language-text",
                element.position,
            )
            resolved == null -> collector.warning(
                IssueCode.CODE_LANGUAGE,
                "Язык «$language» не поддерживается: блок показан без подсветки",
                element.position,
            )
            resolved == CodeLanguage.JSON -> JsonSyntax.firstProblem(text)?.let { problem ->
                val before = text.substring(0, problem.offset)
                val line = before.count { it == '\n' } + 1
                val column = problem.offset - before.lastIndexOf('\n')
                collector.warning(
                    IssueCode.CODE_JSON_INVALID,
                    "JSON в блоке не разбирается — строка $line, столбец $column блока: ${problem.message}",
                    element.position,
                )
            }
        }
    }

    private fun firstForeignTag(nodes: List<MarkupNode>, allowed: Set<String>): MarkupNode.Element? =
        nodes.firstNotNullOfOrNull { node ->
            val element = node as? MarkupNode.Element
            when {
                element == null -> null
                element.name !in allowed -> element
                else -> firstForeignTag(element.children, allowed)
            }
        }

    private fun codeMarkup(nested: MarkupNode.Element) {
        collector.error(
            IssueCode.CODE_MARKUP,
            "Тег <${nested.name}> внутри кода: в коде символы < и & пишутся как &lt; и &amp;",
            nested.position,
        )
    }

    private fun table(element: MarkupNode.Element): PageBlock.Table? {
        val rows = mutableListOf<TableRow>()
        fun collect(nodes: List<MarkupNode>) {
            flatten(nodes).forEach { child ->
                when {
                    child is MarkupNode.Text && child.text.isBlank() -> Unit
                    child is MarkupNode.Element && child.name == "tr" -> rows += TableRow(cells(child))
                    child is MarkupNode.Element && child.name in HtmlTags.tableSections -> collect(child.children)
                    else -> misplaced(child, "Внутри <table> ожидаются строки <tr>: содержимое пропущено")
                }
            }
        }
        collect(element.children)
        return rows.takeIf { it.isNotEmpty() }?.let { PageBlock.Table(it) }
    }

    private fun cells(row: MarkupNode.Element): List<TableCell> =
        flatten(row.children).mapNotNull { child ->
            when {
                child is MarkupNode.Text && child.text.isBlank() -> null
                child is MarkupNode.Element && child.name in HtmlTags.tableCells ->
                    TableCell(header = child.name == "th", content = normalize(inlines(child.children, InlineStyle.Plain)))
                else -> {
                    misplaced(child, "Внутри <tr> ожидаются ячейки <td> и <th>: содержимое пропущено")
                    null
                }
            }
        }

    private fun figure(element: MarkupNode.Element): List<PageBlock> {
        val children = flatten(element.children)
        val image = children.firstOrNull { it is MarkupNode.Element && it.name == "img" } as MarkupNode.Element?
        val caption = children.firstOrNull { it is MarkupNode.Element && it.name == "figcaption" } as MarkupNode.Element?
        if (image == null) {
            misplaced(element, "В <figure> нет <img>: показан как обычный текст")
            val captionText = caption?.let { MarkupNode.Text(it.textContent(), it.position) }
            return blocks(children.filterNot { it == caption } + listOfNotNull(captionText))
        }
        val captionContent = caption?.let { normalize(inlines(it.children, InlineStyle.Plain)) }.orEmpty()
        val rest = children.filterNot { it == image || it == caption || (it is MarkupNode.Text && it.text.isBlank()) }
        if (rest.isNotEmpty()) misplaced(element, "В <figure> одна схема и подпись: остальное показано после схемы")
        return listOfNotNull(image(image, captionContent)) + blocks(rest)
    }

    private fun image(element: MarkupNode.Element, caption: List<PageInline>): PageBlock.Figure? {
        val source = element.attribute("src")?.value?.trim().orEmpty()
        val resolved = ThemePaths.resolve(file, source)
        val problem = when {
            source.isEmpty() -> "У <img> нет атрибута src"
            EXTERNAL_SOURCE.containsMatchIn(source) ->
                "Схема «$source» подключена по адресу: схемы лежат в папке темы, например src=\"diagrams/flow.svg\""
            resolved == null -> "Путь «$source» выходит за папку темы или содержит запрещённые символы"
            ThemePaths.extension(resolved) != SVG_EXTENSION -> "«$source»: поддерживаются только схемы .svg"
            else -> null
        }
        if (problem != null || resolved == null) {
            collector.error(IssueCode.IMAGE_SOURCE, problem.orEmpty(), element.position)
            return null
        }
        imageReferences += Reference(resolved, element.position)
        val alt = element.attribute("alt")?.value?.trim()?.takeIf { it.isNotEmpty() }
        if (alt == null) {
            collector.warning(
                IssueCode.IMAGE_ALT_MISSING,
                "У схемы «$source» нет описания alt: его читает экранный диктор",
                element.position,
            )
        }
        return PageBlock.Figure(source = resolved, alt = alt, caption = caption)
    }

    private fun inlines(nodes: List<MarkupNode>, style: InlineStyle): List<PageInline> =
        flatten(nodes).flatMap { inline(it, style) }

    private fun inline(node: MarkupNode, style: InlineStyle): List<PageInline> {
        if (node is MarkupNode.Text) return listOf(PageInline.Text(node.text.replace(WHITESPACE, " "), style))
        val element = node as MarkupNode.Element
        return when (element.name) {
            "b", "strong" -> inlines(element.children, style.copy(bold = true))
            "i", "em" -> inlines(element.children, style.copy(italic = true))
            "code" -> {
                firstForeignTag(element.children, allowed = HtmlTags.inlineStyles - "code")?.let(::codeMarkup)
                inlines(element.children, style.copy(code = true))
            }
            "br" -> listOf(PageInline.LineBreak)
            "a" -> if (linkDepth > 0) nestedLink(element, style) else link(element, style)
            "img" -> {
                misplaced(element, "<img> внутри текста не поддерживается: поставьте схему отдельным блоком")
                emptyList()
            }
            else -> {
                misplaced(element, "<${element.name}> внутри текста: показан как обычный текст")
                inlines(element.children, style)
            }
        }
    }

    private fun nestedLink(element: MarkupNode.Element, style: InlineStyle): List<PageInline> {
        misplaced(element, "Ссылка внутри ссылки: внутренняя показана как текст")
        return inlines(element.children, style)
    }

    private fun link(element: MarkupNode.Element, style: InlineStyle): List<PageInline> {
        linkDepth++
        val content = try {
            inlines(element.children, style)
        } finally {
            linkDepth--
        }
        val href = element.attribute("href")?.value?.trim()
        if (href.isNullOrEmpty()) {
            collector.warning(IssueCode.LINK_EMPTY, "У ссылки <a> нет адреса href: показан только текст", element.position)
            return content
        }
        val target = linkTarget(href, element) ?: return content
        val download = download(element, target)
        val visibleContent = normalize(content).ifEmpty { listOf(PageInline.Text(href, style)) }
        val space = PageInline.Text(" ", style)
        return listOfNotNull(
            space.takeIf { (content.firstOrNull() as? PageInline.Text)?.text?.startsWith(' ') == true },
            PageInline.Link(target = target, download = download, content = visibleContent),
            space.takeIf { (content.lastOrNull() as? PageInline.Text)?.text?.endsWith(' ') == true },
        )
    }

    private fun linkTarget(href: String, element: MarkupNode.Element): LinkTarget? {
        val lower = href.lowercase()
        return when {
            lower.startsWith(PAGE_SCHEME) -> {
                val id = href.substring(PAGE_SCHEME.length)
                if (ThemeIds.isValid(id)) {
                    pageReferences += Reference(id, element.position)
                    LinkTarget.Page(id)
                } else {
                    collector.error(
                        IssueCode.LINK_PAGE_MISSING,
                        "Ссылка «$href»: после page: нужен id страницы из manifest.json",
                        element.position,
                    )
                    null
                }
            }
            lower.startsWith("https://") && WEB_HOST.matches(href) -> LinkTarget.Web(href)
            lower.startsWith("http://") -> {
                collector.error(IssueCode.LINK_HTTP, "Ссылка «$href» без шифрования: замените http:// на https://", element.position)
                null
            }
            lower.startsWith("mailto:") && href.length > MAILTO_LENGTH -> LinkTarget.Email(href)
            lower.startsWith("tel:") && href.length > TEL_LENGTH -> LinkTarget.Phone(href)
            else -> {
                collector.error(
                    IssueCode.LINK_UNSUPPORTED,
                    "Ссылка «$href» не поддерживается: страницы темы — page:<id>, сайты — https://, ещё mailto: и tel:",
                    element.position,
                )
                null
            }
        }
    }

    private fun download(element: MarkupNode.Element, target: LinkTarget): String? {
        val attribute = element.attribute("download")
        val isPdf = target is LinkTarget.Web && ThemePaths.extension(target.url.substringBefore('#').substringBefore('?')) == PDF_EXTENSION
        val name = attribute?.value?.trim().orEmpty()
        val valid = name.length in 1..MAX_DOWNLOAD_NAME &&
            name.lowercase().endsWith(".$PDF_EXTENSION") &&
            !FORBIDDEN_FILE_NAME_CHARS.containsMatchIn(name)
        when {
            !isPdf && attribute != null -> collector.warning(
                IssueCode.DOWNLOAD_NOT_PDF,
                "Атрибут download действует только у ссылок на PDF и здесь пропущен",
                element.position,
            )
            !isPdf -> Unit
            attribute == null -> collector.warning(
                IssueCode.PDF_DOWNLOAD_MISSING,
                "У ссылки на PDF нет атрибута download: имя файла возьмётся из ответа сервера",
                element.position,
            )
            !valid -> collector.error(
                IssueCode.PDF_DOWNLOAD_INVALID,
                "download=\"$name\": нужно имя файла с окончанием .pdf, без папок и символов \\ / : * ? \" < > |",
                element.position,
            )
        }
        return name.takeIf { isPdf && attribute != null && valid }
    }

    private fun flatten(nodes: List<MarkupNode>): List<MarkupNode> = nodes.flatMap { node ->
        if (node !is MarkupNode.Element) return@flatMap listOf(node)
        when (node.name) {
            in HtmlTags.wrappers -> {
                collector.error(
                    IssueCode.PAGE_WRAPPER,
                    "<${node.name}> не нужен: страница темы — фрагмент без html, head и body",
                    node.position,
                )
                if (node.name == "head") emptyList() else flatten(node.children)
            }
            in HtmlTags.dropped -> {
                collector.warning(IssueCode.TAG_UNKNOWN, "<${node.name}> не поддерживается и пропущен вместе с содержимым", node.position)
                emptyList()
            }
            !in HtmlTags.supported -> {
                collector.warning(
                    IssueCode.TAG_UNKNOWN,
                    "<${node.name}> не поддерживается: содержимое показано как обычный текст",
                    node.position,
                )
                flatten(node.children)
            }
            else -> {
                checkAttributes(node)
                listOf(node)
            }
        }
    }

    private fun checkAttributes(element: MarkupNode.Element) {
        val allowed = HtmlTags.globalAttributes + HtmlTags.attributesByTag[element.name].orEmpty()
        element.attributes.forEach { attribute ->
            val message = when {
                attribute.name.startsWith("on") -> "Атрибут ${attribute.name} пропущен: скрипты в темах не выполняются"
                attribute.name == "style" -> "Атрибут style пропущен: оформление задаёт приложение"
                attribute.name !in allowed -> "Атрибут ${attribute.name} у <${element.name}> не поддерживается и пропущен"
                else -> null
            }
            if (message != null) collector.warning(IssueCode.ATTRIBUTE_UNSUPPORTED, message, attribute.position)
        }
    }

    private fun misplaced(node: MarkupNode, message: String) {
        collector.warning(IssueCode.TAG_MISPLACED, message, node.position)
    }

    private fun MarkupNode.Element.isInline() = name in HtmlTags.inlineStyles || name == "a" || name == "br"

    private fun normalize(items: List<PageInline>): List<PageInline> {
        val result = mutableListOf<PageInline>()
        val text = StringBuilder()
        var textStyle: InlineStyle? = null
        fun flushText() {
            textStyle?.let { result += PageInline.Text(text.toString(), it) }
            text.clear()
            textStyle = null
        }
        var afterSpace = true
        items.forEach { item ->
            when (item) {
                is PageInline.Text -> {
                    val piece = if (afterSpace) item.text.trimStart(' ') else item.text
                    if (piece.isEmpty()) return@forEach
                    if (textStyle != item.style) flushText()
                    textStyle = item.style
                    text.append(piece)
                    afterSpace = piece.endsWith(' ')
                }
                PageInline.LineBreak -> {
                    flushText()
                    trimTrailingSpace(result)
                    result += item
                    afterSpace = true
                }
                is PageInline.Link -> {
                    flushText()
                    result += item
                    afterSpace = false
                }
            }
        }
        flushText()
        trimTrailingSpace(result)
        return result
    }

    private fun trimTrailingSpace(items: MutableList<PageInline>) {
        val last = items.lastOrNull() as? PageInline.Text ?: return
        val trimmed = last.text.trimEnd(' ')
        if (trimmed.isEmpty()) items.removeAt(items.lastIndex) else items[items.lastIndex] = last.copy(text = trimmed)
    }

    private companion object {
        const val PAGE_SCHEME = "page:"
        const val LANGUAGE_PREFIX = "language-"
        const val SVG_EXTENSION = "svg"
        const val PDF_EXTENSION = "pdf"
        const val MAX_DOWNLOAD_NAME = 120
        const val MAILTO_LENGTH = 7
        const val TEL_LENGTH = 4
        val WHITESPACE = Regex("[ \\t\\r\\n\\f]+")
        val WEB_HOST = Regex("(?i)https://[^\\s/?#]+([/?#].*)?")
        val EXTERNAL_SOURCE = Regex("(?i)^([a-z][a-z0-9+.-]*:|//)")
        val FORBIDDEN_FILE_NAME_CHARS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
    }
}

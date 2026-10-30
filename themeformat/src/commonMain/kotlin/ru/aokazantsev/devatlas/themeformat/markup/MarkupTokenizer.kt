package ru.aokazantsev.devatlas.themeformat.markup

internal class MarkupTokenizer(
    private val source: String,
    private val caseSensitive: Boolean,
    private val rawTextElements: Set<String>,
) {
    private val tokens = mutableListOf<MarkupToken>()
    private val problems = mutableListOf<MarkupProblem>()
    private val lineStarts = buildLineStarts()
    private var index = 0
    private val text = StringBuilder()
    private var textStart = 0
    private var stopped = false

    fun tokenize(): Pair<List<MarkupToken>, List<MarkupProblem>> {
        while (index < source.length && !stopped) {
            if (source[index] == '<') readMarkup() else appendText(index + 1)
        }
        flushText()
        return tokens to problems
    }

    private fun readMarkup() {
        when {
            source.startsWith("<!--", index) -> skipUntil("-->", "Комментарий <!-- не закрыт")
            source.startsWith("<![CDATA[", index) -> readCdata()
            source.startsWith("<!", index) && caseSensitive -> {
                problem(MarkupProblemKind.SYNTAX, "Объявления <!DOCTYPE и <!ENTITY в SVG не поддерживаются", index)
                skipUntil(">", "Объявление <! не закрыто")
            }
            source.startsWith("<!", index) || source.startsWith("<?", index) -> skipUntil(">", "Объявление <! не закрыто")
            peek(1) == '/' && peek(2).isAsciiLetter() -> readEndTag()
            peek(1).isAsciiLetter() -> readStartTag()
            caseSensitive -> {
                problem(MarkupProblemKind.SYNTAX, "Символ < в SVG записывается как &lt;", index)
                appendText(index + 1)
            }
            else -> appendText(index + 1)
        }
    }

    private fun readCdata() {
        val start = index
        val end = source.indexOf("]]>", index)
        if (end < 0) {
            problem(MarkupProblemKind.SYNTAX, "Блок CDATA не закрыт", start)
            index = source.length
            return
        }
        flushText()
        tokens += MarkupToken.Text(source.substring(start + CDATA_OPEN.length, end), position(start))
        index = end + CDATA_CLOSE.length
    }

    private fun readEndTag() {
        flushText()
        val start = index
        index += 2
        val name = readName()
        skipWhitespace()
        if (peek(0) != '>') {
            problem(MarkupProblemKind.SYNTAX, "Закрывающий тег </$name не закрыт символом >", start)
            return
        }
        index++
        tokens += MarkupToken.EndTag(name, position(start))
    }

    private fun readStartTag() {
        flushText()
        val start = index
        index++
        val name = readName()
        val attributes = mutableListOf<MarkupAttribute>()
        val names = mutableSetOf<String>()
        var selfClosing = false
        var closed = false
        while (!closed && index < source.length) {
            skipWhitespace()
            val char = peek(0)
            when {
                char == '>' -> {
                    index++
                    closed = true
                }
                char == '/' && peek(1) == '>' -> {
                    selfClosing = true
                    index += 2
                    closed = true
                }
                char == '/' -> index++
                char == '<' -> break
                char != null -> readAttribute(attributes, names)
            }
        }
        if (!closed) {
            problem(MarkupProblemKind.SYNTAX, "Тег <$name не закрыт символом >", start)
            return
        }
        tokens += MarkupToken.StartTag(name, attributes, selfClosing, position(start))
        if (!selfClosing && name.lowercase() in rawTextElements) readRawText(name)
    }

    private fun readAttribute(attributes: MutableList<MarkupAttribute>, names: MutableSet<String>) {
        val start = index
        val nameStart = index
        while (index < source.length && !source[index].isWhitespace() && source[index] !in "=>/<") index++
        if (index == nameStart) {
            index++
            return
        }
        val name = normalize(source.substring(nameStart, index))
        skipWhitespace()
        var value: String? = null
        var quoted = false
        if (peek(0) == '=') {
            index++
            skipWhitespace()
            quoted = peek(0) == '"' || peek(0) == '\''
            value = readAttributeValue(start)
        }
        val duplicate = !names.add(name)
        if (caseSensitive) checkXmlAttribute(name, value, quoted, duplicate, start)
        if (!duplicate) attributes += MarkupAttribute(name, value?.let(Entities::decode), position(start))
    }

    private fun checkXmlAttribute(name: String, value: String?, quoted: Boolean, duplicate: Boolean, at: Int) {
        val message = when {
            value == null -> "У атрибута $name нет значения"
            !quoted -> "Значение атрибута $name без кавычек"
            duplicate -> "Атрибут $name повторён"
            else -> null
        }
        if (message != null) problem(MarkupProblemKind.SYNTAX, message, at)
        if (value != null && '<' in value) problem(MarkupProblemKind.SYNTAX, "Символ < в SVG записывается как &lt;", at)
        value?.let(Entities::undefinedInXml)?.let { reportXmlReference(it, at) }
    }

    private fun reportXmlReference(match: MatchResult, at: Int) {
        val message = if (match.groupValues[1].isEmpty()) {
            "Символ & в SVG записывается как &amp;"
        } else {
            "Спецсимвола &${match.groupValues[1]} нет в XML: запишите его числом, например &#160; вместо &nbsp;"
        }
        problem(MarkupProblemKind.SYNTAX, message, at)
    }

    private fun readAttributeValue(attributeStart: Int): String {
        val quote = peek(0)
        if (quote == '"' || quote == '\'') {
            val end = source.indexOf(quote, index + 1)
            if (end < 0) {
                problem(MarkupProblemKind.SYNTAX, "Кавычка значения атрибута не закрыта", attributeStart)
                val value = source.substring(index + 1)
                index = source.length
                return value
            }
            val value = source.substring(index + 1, end)
            index = end + 1
            return value
        }
        val valueStart = index
        while (index < source.length && !source[index].isWhitespace() && source[index] !in "<>") index++
        return source.substring(valueStart, index)
    }

    private fun readRawText(name: String) {
        val start = index
        val end = indexOfIgnoreCase("</$name", index)
        if (end < 0) {
            problem(MarkupProblemKind.SYNTAX, "Тег <$name> не закрыт: если это текст, а не тег, пишите &lt;$name&gt;", start)
            return
        }
        if (end > start) tokens += MarkupToken.Text(source.substring(start, end), position(start))
        index = end
    }

    private fun readName(): String {
        val start = index
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] in NAME_PUNCTUATION)) index++
        return normalize(source.substring(start, index))
    }

    private fun appendText(until: Int) {
        if (text.isEmpty()) textStart = index
        val next = source.indexOf('<', until).let { if (it < 0) source.length else it }
        text.append(source, index, next)
        index = next
    }

    private fun flushText() {
        if (text.isEmpty()) return
        val raw = text.toString()
        if (caseSensitive) {
            Entities.undefinedInXml(raw)?.let { reportXmlReference(it, textStart + it.range.first) }
        } else {
            Entities.unknownNamed(raw)?.let { match ->
                problem(
                    MarkupProblemKind.ENTITY,
                    "Спецсимвол ${match.value} не поддерживается и показан как есть: запишите его числом, например &#8734;",
                    textStart + match.range.first,
                )
            }
        }
        tokens += MarkupToken.Text(Entities.decode(raw), position(textStart))
        text.clear()
    }

    private fun skipUntil(terminator: String, message: String) {
        flushText()
        val end = source.indexOf(terminator, index + 2)
        if (end < 0) {
            problem(MarkupProblemKind.SYNTAX, message, index)
            index = source.length
        } else {
            index = end + terminator.length
        }
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    private fun peek(offset: Int): Char? = source.getOrNull(index + offset)

    private fun Char?.isAsciiLetter() = this != null && (this in 'a'..'z' || this in 'A'..'Z')

    private fun normalize(name: String) = if (caseSensitive) name else name.lowercase()

    private fun indexOfIgnoreCase(needle: String, from: Int): Int {
        var candidate = from
        while (candidate <= source.length - needle.length) {
            if (source.regionMatches(candidate, needle, 0, needle.length, ignoreCase = true)) return candidate
            candidate++
        }
        return -1
    }

    private fun problem(kind: MarkupProblemKind, message: String, at: Int) {
        if (problems.size >= MarkupProblem.LIMIT) return
        problems += MarkupProblem(kind, message, position(at))
        if (problems.size == MarkupProblem.LIMIT - 1) {
            problems += MarkupProblem(MarkupProblemKind.SYNTAX, "Слишком много ошибок разметки: дальше файл не проверяется", position(at))
            stopped = true
        }
    }

    private fun buildLineStarts(): IntArray {
        val starts = mutableListOf(0)
        source.forEachIndexed { offset, char -> if (char == '\n') starts += offset + 1 }
        return starts.toIntArray()
    }

    private fun position(offset: Int): SourcePosition {
        var low = 0
        var high = lineStarts.size - 1
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (lineStarts[middle] <= offset) low = middle else high = middle - 1
        }
        return SourcePosition(line = low + 1, column = offset - lineStarts[low] + 1)
    }

    private companion object {
        const val CDATA_OPEN = "<![CDATA["
        const val CDATA_CLOSE = "]]>"
        const val NAME_PUNCTUATION = "-_:."
    }
}

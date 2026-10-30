package ru.aokazantsev.devatlas.themeformat.code.lexer

import ru.aokazantsev.devatlas.themeformat.code.CodeTokenKind
import ru.aokazantsev.devatlas.themeformat.code.lexer.LexerState.Companion.isIdentifierPart
import ru.aokazantsev.devatlas.themeformat.code.lexer.LexerState.Companion.isIdentifierStart

internal class ProgramScan(private val rules: ProgramRules, private val s: LexerState) {

    private var pendingHeredoc: String? = null

    private val scanners: List<() -> Boolean> = listOf(
        ::heredocBody,
        ::escaped,
        ::heredoc,
        ::lineComment,
        ::blockComment,
        ::preprocessorLine,
        ::hashSyntax,
        ::marker,
        ::string,
        ::character,
        ::variable,
        ::annotation,
        ::symbol,
        ::regex,
        ::number,
        ::word,
    )

    fun run() {
        if (startsWithMarkup()) markupGap()
        while (!s.atEnd) {
            if (scanners.none { it() }) s.pos++
        }
    }

    private fun heredocBody(): Boolean {
        val terminator = pendingHeredoc ?: return false
        if (s.char() != '\n') return false
        pendingHeredoc = null
        val bodyStart = s.pos + 1
        var lineStart = bodyStart
        while (lineStart < s.end && s.text.substring(lineStart, s.lineEnd(lineStart)).trim() != terminator) {
            lineStart = s.lineEnd(lineStart) + 1
        }
        s.emit(bodyStart, minOf(lineStart, s.end), CodeTokenKind.STRING)
        s.pos = minOf(lineStart, s.end)
        if (!s.atEnd) s.emitTo(s.lineEnd(), CodeTokenKind.META)
        return true
    }

    private fun escaped(): Boolean {
        if (!rules.escapesOutsideStrings || s.char() != '\\') return false
        s.pos = minOf(s.pos + 2, s.end)
        return true
    }

    private fun heredoc(): Boolean {
        if (rules.heredocs == HeredocStyle.NONE || !s.startsWith("<<")) return false
        if (s.char(2) == '<' || s.previous() == '<' || !heredocAllowedHere()) return false
        var index = s.pos + 2
        if (s.charAt(index) == '-' || s.charAt(index) == '~') index++
        if (rules.heredocs == HeredocStyle.SHELL) index = s.skipSpaces(index)
        val quote = s.charAt(index)?.takeIf { it == '\'' || it == '"' }
        if (quote != null) index++
        if (!isIdentifierStart(s.charAt(index))) return false
        val wordEnd = s.identifierEnd(index)
        pendingHeredoc = s.text.substring(index, wordEnd)
        s.emitTo(if (quote != null && s.charAt(wordEnd) == quote) wordEnd + 1 else wordEnd, CodeTokenKind.META)
        return true
    }

    private fun heredocAllowedHere(): Boolean {
        if (rules.heredocs == HeredocStyle.SHELL) return !insideArithmetic()
        val previous = s.previous()
        return !isIdentifierPart(previous) && previous != ')' && previous != ']'
    }

    private fun insideArithmetic(): Boolean {
        var lineStart = s.pos
        while (lineStart > s.start && s.text[lineStart - 1] != '\n') lineStart--
        val before = s.text.substring(lineStart, s.pos)
        return occurrences(before, "((") > occurrences(before, "))")
    }

    private fun occurrences(text: String, part: String): Int {
        var count = 0
        var index = text.indexOf(part)
        while (index != -1) {
            count++
            index = text.indexOf(part, index + part.length)
        }
        return count
    }

    private fun lineComment(): Boolean {
        if (rules.lineComments.none { s.startsWith(it) }) return false
        if (rules.commentAfterSpaceOnly && s.previous()?.isWhitespace() == false) return false
        s.emitTo(lineCommentEnd(), CodeTokenKind.COMMENT)
        return true
    }

    private fun lineCommentEnd(): Int {
        val lineEnd = s.lineEnd()
        val close = rules.markupAfter ?: return lineEnd
        return (s.pos until lineEnd).firstOrNull { s.startsWith(close, it) } ?: lineEnd
    }

    private fun blockComment(): Boolean {
        val comment = rules.blockComment ?: return false
        if (!s.startsWith(comment.open) || comment.atLineStart && !s.isLineStart()) return false
        var depth = 1
        var index = s.pos + comment.open.length
        while (index < s.end && depth > 0) {
            when {
                s.startsWith(comment.close, index) -> {
                    depth--
                    index += comment.close.length
                }
                comment.nested && s.startsWith(comment.open, index) -> {
                    depth++
                    index += comment.open.length
                }
                else -> index++
            }
        }
        s.emitTo(index, CodeTokenKind.COMMENT)
        return true
    }

    private fun preprocessorLine(): Boolean {
        if (!rules.preprocessor || s.char() != '#' || !s.isLineStart()) return false
        var lineEnd = s.lineEnd()
        while (lineEnd < s.end && s.text[lineEnd - 1] == '\\') lineEnd = s.lineEnd(lineEnd + 1)
        s.emitTo(lineEnd, CodeTokenKind.META)
        return true
    }

    private fun hashSyntax(): Boolean {
        if (s.char() != '#') return false
        val end = when {
            rules.hashAttributes && s.startsWith("#[") -> s.afterClosing(s.pos + 1, '[', ']')
            rules.hashAttributes && s.startsWith("#![") -> s.afterClosing(s.pos + 2, '[', ']')
            rules.hashDirectives && isIdentifierStart(s.char(1)) -> s.identifierEnd(s.pos + 1)
            else -> return false
        }
        s.emitTo(end, CodeTokenKind.META)
        return true
    }

    private fun marker(): Boolean {
        val marker = rules.markers.firstOrNull { s.startsWithIgnoreCase(it) } ?: return false
        s.emitTo(s.pos + marker.length, CodeTokenKind.META)
        if (marker == rules.markupAfter) markupGap()
        return true
    }

    private fun startsWithMarkup(): Boolean {
        if (rules.markupAfter == null) return false
        val first = s.skipWhitespace(s.pos)
        if (s.charAt(first) != '<' || s.startsWith(MARKUP_GAP_END, first)) return false
        return s.text.indexOf(MARKUP_GAP_END, first) in first until s.end
    }

    private fun markupGap() {
        var gapEnd = s.pos
        while (gapEnd < s.end && !s.startsWith(MARKUP_GAP_END, gapEnd)) gapEnd++
        MarkupLexer().tokenize(s.embedded(s.pos, gapEnd))
        s.pos = gapEnd
    }

    private fun string(): Boolean {
        val rule = rules.strings.firstOrNull { s.startsWith(it.open) } ?: return false
        scanString(rule, bodyStart = s.pos + rule.open.length)
        return true
    }

    private fun character(): Boolean {
        if (!rules.lifetimes || s.char() != '\'') return false
        val closing = when {
            s.char(1) == '\\' -> (s.pos + ESCAPED_CHAR_BODY..s.pos + MAX_CHAR_LITERAL).firstOrNull { s.charAt(it) == '\'' }
            s.char(2) == '\'' -> s.pos + 2
            s.char(1)?.isHighSurrogate() == true && s.char(SURROGATE_CLOSE) == '\'' -> s.pos + SURROGATE_CLOSE
            else -> null
        }
        if (closing != null) {
            s.emitTo(closing + 1, CodeTokenKind.STRING)
            return true
        }
        val end = s.identifierEnd(s.pos + 1)
        if (end == s.pos + 1) return false
        s.emitTo(end, CodeTokenKind.TYPE)
        return true
    }

    private fun variable(): Boolean {
        val prefix = s.char() ?: return false
        if (prefix !in rules.variablePrefixes) return false
        val next = s.char(1)
        val end = when {
            next == '{' -> s.afterClosing(s.pos + 1, '{', '}', s.lineEnd())
            isIdentifierStart(next) -> s.identifierEnd(s.pos + 1)
            next == prefix && isIdentifierStart(s.char(2)) -> s.identifierEnd(s.pos + 2)
            prefix == '$' && next != null && next in SPECIAL_VARIABLES -> s.pos + 2
            else -> return false
        }
        s.emitTo(end, CodeTokenKind.VARIABLE)
        return true
    }

    private fun annotation(): Boolean {
        val prefix = rules.annotationPrefix ?: return false
        if (s.char() != prefix || !isIdentifierStart(s.char(1))) return false
        var end = s.identifierEnd(s.pos + 1, rules.identifierExtra)
        if (s.text.substring(s.pos, end) in rules.keywords) {
            s.emitTo(end, CodeTokenKind.KEYWORD)
            return true
        }
        while (s.charAt(end) == '.' && isIdentifierStart(s.charAt(end + 1))) end = s.identifierEnd(end + 1)
        s.emitTo(end, CodeTokenKind.META)
        return true
    }

    private fun symbol(): Boolean {
        if (!rules.symbols || s.char() != ':' || !isIdentifierStart(s.char(1))) return false
        val previous = s.previous()
        if (previous == ':' || isIdentifierPart(previous)) return false
        var end = s.identifierEnd(s.pos + 1)
        if (s.charAt(end) == '?' || s.charAt(end) == '!') end++
        s.emitTo(end, CodeTokenKind.NUMBER)
        return true
    }

    private fun regex(): Boolean {
        if (!rules.regexLiterals || s.char() != '/') return false
        if (s.char(1) == '/' || s.char(1) == '*' || !regexAllowed()) return false
        val end = regexEnd() ?: return false
        s.emitTo(end, CodeTokenKind.STRING)
        return true
    }

    private fun regexEnd(): Int? {
        var index = s.pos + 1
        var inClass = false
        val limit = s.lineEnd()
        while (index < limit) {
            when (s.text[index]) {
                '\\' -> index++
                '[' -> inClass = true
                ']' -> inClass = false
                '/' -> if (!inClass) return s.identifierEnd(index + 1)
            }
            index++
        }
        return null
    }

    private fun regexAllowed(): Boolean {
        var index = s.pos - 1
        while (index >= s.start && s.text[index].isWhitespace()) index--
        if (index < s.start) return true
        val last = s.text[index]
        if (!isIdentifierPart(last, rules.identifierExtra)) return last in REGEX_PRECEDERS
        var wordStart = index
        while (wordStart > s.start && isIdentifierPart(s.text[wordStart - 1], rules.identifierExtra)) wordStart--
        return s.text.substring(wordStart, index + 1) in REGEX_KEYWORDS
    }

    private fun number(): Boolean {
        val char = s.char() ?: return false
        val previous = s.previous()
        val fraction = char == '.' && s.char(1)?.isDigit() == true && previous != '.' && !isIdentifierPart(previous)
        if (!char.isDigit() && !fraction) return false
        s.emitTo(s.numberEnd(s.pos), CodeTokenKind.NUMBER)
        return true
    }

    private fun word(): Boolean {
        if (!isIdentifierStart(s.char(), rules.identifierExtra)) return false
        val end = s.identifierEnd(s.pos, rules.identifierExtra)
        val word = s.text.substring(s.pos, end)
        if (prefixedString(word, end) || rawHashString(word, end)) return true
        val kind = classify(word)
        when {
            kind != null -> s.emitTo(end, kind)
            rules.macros && s.charAt(end) == '!' && s.charAt(end + 1) != '=' -> s.emitTo(end + 1, CodeTokenKind.META)
            else -> s.pos = end
        }
        return true
    }

    private fun classify(word: String): CodeTokenKind? {
        val key = if (rules.ignoreCase) word.lowercase() else word
        return when {
            key in rules.keywords -> CodeTokenKind.KEYWORD
            key in rules.literals -> CodeTokenKind.NUMBER
            key in rules.types -> CodeTokenKind.TYPE
            rules.capitalizedTypes && word.first().isUpperCase() && word.any { it.isLowerCase() } -> CodeTokenKind.TYPE
            else -> null
        }
    }

    private fun prefixedString(word: String, end: Int): Boolean {
        if (word.lowercase() !in rules.stringPrefixes) return false
        val rule = rules.strings.firstOrNull { s.startsWith(it.open, end) } ?: return false
        scanString(rule, bodyStart = end + rule.open.length)
        return true
    }

    private fun rawHashString(word: String, end: Int): Boolean {
        if (!rules.rawHashStrings || word !in RAW_STRING_PREFIXES) return false
        var quote = end
        while (s.charAt(quote) == '#') quote++
        if (s.charAt(quote) != '"') return false
        val closing = "\"" + "#".repeat(quote - end)
        val found = s.text.indexOf(closing, quote + 1)
        val stringEnd = if (found == -1 || found + closing.length > s.end) s.end else found + closing.length
        s.emitTo(stringEnd, CodeTokenKind.STRING)
        return true
    }

    private fun scanString(rule: StringRule, bodyStart: Int) {
        var segment = s.pos
        var index = bodyStart
        var closed = false
        while (index < s.end && !closed) {
            val char = s.text[index]
            when {
                templateStart(rule, index) -> {
                    val templateEnd = templateEnd(rule, index)
                    emitString(rule, segment, index)
                    if (rule.kind != null) s.emit(index, templateEnd, CodeTokenKind.VARIABLE)
                    segment = templateEnd
                    index = templateEnd
                }
                rule.escapes && char == '\\' -> index += 2
                s.startsWith(rule.close, index) -> {
                    index += rule.close.length
                    closed = true
                }
                !rule.multiline && char == '\n' -> closed = true
                else -> index++
            }
        }
        index = minOf(index, s.end)
        emitString(rule, segment, index)
        s.pos = index
    }

    private fun emitString(rule: StringRule, from: Int, to: Int) {
        rule.kind?.let { s.emit(from, to, it) }
    }

    private fun templateStart(rule: StringRule, index: Int): Boolean {
        val next = s.charAt(index + 1)
        return when (rule.templates) {
            StringTemplates.NONE -> false
            StringTemplates.DOLLAR -> s.text[index] == '$' && (next == '{' || isIdentifierStart(next))
            StringTemplates.DOLLAR_BRACE -> s.text[index] == '$' && next == '{'
            StringTemplates.BACKSLASH_PAREN -> s.text[index] == '\\' && next == '('
        }
    }

    private fun templateEnd(rule: StringRule, index: Int): Int {
        val limit = if (rule.multiline) s.end else s.lineEnd(index)
        return when (s.charAt(index + 1)) {
            '{' -> s.afterClosing(index + 1, '{', '}', limit)
            '(' -> s.afterClosing(index + 1, '(', ')', limit)
            else -> s.identifierEnd(index + 1)
        }
    }

    private companion object {
        const val ESCAPED_CHAR_BODY = 3
        const val MAX_CHAR_LITERAL = 12
        const val SURROGATE_CLOSE = 3
        const val MARKUP_GAP_END = "<?"
        const val SPECIAL_VARIABLES = "@*#?$!-0123456789"
        const val REGEX_PRECEDERS = "(,=:[!&|?{};+-*%<>~^"
        val REGEX_KEYWORDS = words(
            "return typeof case do else in of new delete void throw yield await when if unless elsif while until and or not",
        )
        val RAW_STRING_PREFIXES = setOf("r", "br")
    }
}

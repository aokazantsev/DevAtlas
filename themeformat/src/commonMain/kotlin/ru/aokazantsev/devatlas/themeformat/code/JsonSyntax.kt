package ru.aokazantsev.devatlas.themeformat.code

object JsonSyntax {

    fun firstProblem(text: String): JsonProblem? = JsonScan(text).run()

    private class JsonScan(private val text: String) {
        private var pos = 0
        private val containers = ArrayDeque<Char>()
        private var problem: JsonProblem? = null

        fun run(): JsonProblem? {
            skipWhitespace()
            if (pos >= text.length) return JsonProblem(pos, "блок пустой, а JSON должен содержать значение")
            var expect = Expect.VALUE
            while (problem == null && expect != Expect.DONE) {
                skipWhitespace()
                expect = when (expect) {
                    Expect.VALUE -> value()
                    Expect.KEY -> key()
                    Expect.AFTER_VALUE -> afterValue()
                    Expect.DONE -> Expect.DONE
                }
            }
            return problem
        }

        private fun value(): Expect {
            val char = text.getOrNull(pos) ?: return fail("ожидалось значение, а блок закончился")
            return when {
                char == '{' -> open('{', '}', Expect.KEY)
                char == '[' -> open('[', ']', Expect.VALUE)
                char == '"' -> string()
                char == '-' || char.isDigit() -> number()
                char.isLetter() -> literal()
                (char == ']' || char == '}') && afterComma() -> fail("лишняя запятая перед «$char»")
                else -> fail("ожидалось значение, а стоит «$char»")
            }
        }

        private fun afterComma(): Boolean {
            var index = pos - 1
            while (index >= 0 && text[index] in WHITESPACE) index--
            return index >= 0 && text[index] == ','
        }

        private fun open(char: Char, close: Char, next: Expect): Expect {
            pos++
            skipWhitespace()
            if (text.getOrNull(pos) == close) {
                pos++
                return Expect.AFTER_VALUE
            }
            containers.addLast(char)
            return next
        }

        private fun key(): Expect {
            if (text.getOrNull(pos) == '}') return fail("лишняя запятая перед «}»")
            if (text.getOrNull(pos) != '"') return fail("ожидалось имя поля в двойных кавычках")
            if (string() == Expect.DONE) return Expect.DONE
            skipWhitespace()
            if (text.getOrNull(pos) != ':') return fail("после имени поля ожидалось «:»")
            pos++
            return Expect.VALUE
        }

        private fun afterValue(): Expect {
            val container = containers.lastOrNull()
            val char = text.getOrNull(pos)
            return when {
                container == null && char == null -> Expect.DONE
                container == null -> fail("лишний текст после конца JSON")
                char == ',' -> {
                    pos++
                    if (container == '{') Expect.KEY else Expect.VALUE
                }
                char == closing(container) -> {
                    pos++
                    containers.removeLast()
                    Expect.AFTER_VALUE
                }
                else -> fail("ожидалась запятая или «${closing(container)}»")
            }
        }

        private fun string(): Expect {
            pos++
            while (problem == null && pos < text.length && text[pos] != '"') {
                val char = text[pos]
                when {
                    char < ' ' -> fail("перенос строки или управляющий символ внутри строки")
                    char == '\\' -> escape()
                    else -> pos++
                }
            }
            if (problem != null) return Expect.DONE
            if (pos >= text.length) return fail("строка не закрыта кавычкой")
            pos++
            return Expect.AFTER_VALUE
        }

        private fun escape() {
            val next = text.getOrNull(pos + 1)
            when {
                next == 'u' && unicodeEscapeAhead() -> pos += UNICODE_ESCAPE
                next == 'u' -> fail("после \\u нужны четыре шестнадцатеричные цифры")
                next != null && next in SIMPLE_ESCAPES -> pos += 2
                else -> fail("неизвестная последовательность «\\${next ?: ""}»")
            }
        }

        private fun unicodeEscapeAhead(): Boolean {
            val hex = text.substring(minOf(pos + 2, text.length), minOf(pos + UNICODE_ESCAPE, text.length))
            return hex.length == HEX_DIGITS && hex.all { it.isHexDigit() }
        }

        private fun number(): Expect {
            val match = NUMBER.matchAt(text, pos) ?: return fail("неверное число")
            pos += match.value.length
            return Expect.AFTER_VALUE
        }

        private fun literal(): Expect {
            val end = (pos until text.length).firstOrNull { !text[it].isLetterOrDigit() } ?: text.length
            val word = text.substring(pos, end)
            if (word !in LITERALS) return fail("«$word» — не значение JSON: строки пишутся в двойных кавычках")
            pos = end
            return Expect.AFTER_VALUE
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos] in WHITESPACE) pos++
        }

        private fun fail(message: String): Expect {
            problem = JsonProblem(pos, message)
            return Expect.DONE
        }

        private fun closing(container: Char): Char = if (container == '{') '}' else ']'

        private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
    }

    private enum class Expect { VALUE, KEY, AFTER_VALUE, DONE }

    private const val UNICODE_ESCAPE = 6
    private const val HEX_DIGITS = 4
    private const val SIMPLE_ESCAPES = "\"\\/bfnrt"
    private const val WHITESPACE = " \t\n\r"
    private val LITERALS = setOf("true", "false", "null")
    private val NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][-+]?[0-9]+)?")
}

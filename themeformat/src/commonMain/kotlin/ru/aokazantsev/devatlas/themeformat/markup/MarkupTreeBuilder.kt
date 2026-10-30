package ru.aokazantsev.devatlas.themeformat.markup

internal class MarkupTreeBuilder(private val dialect: MarkupDialect) {

    private val root = OpenElement(MarkupToken.StartTag(ROOT, emptyList(), false, SourcePosition(1, 1)))
    private val stack = mutableListOf(root)
    private val problems = mutableListOf<MarkupProblem>()
    private var depthReported = false
    private var skippedOpen = 0

    fun build(tokens: List<MarkupToken>): Pair<List<MarkupNode>, List<MarkupProblem>> {
        tokens.forEach { token ->
            when (token) {
                is MarkupToken.Text -> stack.last().children += MarkupNode.Text(token.text, token.position)
                is MarkupToken.StartTag -> start(token)
                is MarkupToken.EndTag -> end(token)
            }
        }
        while (stack.size > 1) {
            val open = stack.last()
            if (open.token.name !in dialect.optionalEnd) unclosed(open)
            pop()
        }
        return root.children.toList() to problems
    }

    private fun start(token: MarkupToken.StartTag) {
        dialect.impliedEnds(token.name).forEach { rule -> closeImplied(rule) }
        val open = OpenElement(token)
        val tooDeep = stack.size > MAX_DEPTH
        if (tooDeep && !depthReported) {
            depthReported = true
            report(MarkupProblem(MarkupProblemKind.SYNTAX, "Вложенность тегов глубже $MAX_DEPTH уровней", token.position))
        }
        if (tooDeep && !token.selfClosing && token.name !in dialect.voidElements) skippedOpen++
        if (tooDeep || token.selfClosing || token.name in dialect.voidElements) {
            stack.last().children += open.toNode()
        } else {
            stack += open
        }
    }

    private fun end(token: MarkupToken.EndTag) {
        if (token.name in dialect.voidElements) return
        if (skippedOpen > 0) {
            skippedOpen--
            return
        }
        val target = stack.indexOfLast { it.token.name == token.name }
        if (target <= 0) {
            report(MarkupProblem(MarkupProblemKind.STRUCTURE, "Лишний закрывающий тег </${token.name}>", token.position))
            return
        }
        while (stack.size > target + 1) {
            val open = stack.last()
            if (open.token.name !in dialect.optionalEnd) unclosed(open)
            pop()
        }
        pop()
    }

    private fun closeImplied(rule: ImpliedEnd) {
        for (depth in stack.indices.reversed()) {
            if (depth == 0) return
            val name = stack[depth].token.name
            if (name in rule.boundaries) return
            if (name in rule.targets) {
                while (stack.size > depth) pop()
                return
            }
        }
    }

    private fun pop() {
        val open = stack.removeAt(stack.lastIndex)
        stack.last().children += open.toNode()
    }

    private fun unclosed(open: OpenElement) {
        report(MarkupProblem(MarkupProblemKind.STRUCTURE, "Тег <${open.token.name}> не закрыт", open.token.position))
    }

    private companion object {
        const val ROOT = "#root"
        const val MAX_DEPTH = 64
    }

    private fun report(problem: MarkupProblem) {
        if (problems.size < MarkupProblem.LIMIT) problems += problem
    }
}

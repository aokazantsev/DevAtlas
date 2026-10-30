package ru.aokazantsev.devatlas.themeformat.markup

data class MarkupProblem(
    val kind: MarkupProblemKind,
    val message: String,
    val position: SourcePosition,
) {
    companion object {
        const val LIMIT = 100
    }
}

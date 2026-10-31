package ru.aokazantsev.devatlas.themeformat.code.lexer

internal object JvmFamilyRules {

    val kotlin = ProgramRules(
        keywords = words(
            "as break class continue do else for fun if in interface is object package return super this throw try typealias " +
                "typeof val var when while by catch constructor finally import init where abstract actual annotation companion " +
                "const crossinline data enum expect external final infix inline inner internal lateinit noinline open operator " +
                "out override private protected public reified sealed suspend tailrec vararg",
        ),
        blockComment = BlockComment("/*", "*/", nested = true),
        strings = listOf(
            StringRule("\"\"\"", escapes = false, multiline = true, templates = StringTemplates.DOLLAR),
            StringRule("\"", templates = StringTemplates.DOLLAR),
            StringRule("'"),
            StringRule("`", kind = null),
        ),
        annotationPrefix = '@',
        capitalizedTypes = true,
    )

    val java = ProgramRules(
        keywords = words(
            "abstract assert break case catch class const continue default do else enum extends final finally for goto if " +
                "implements import instanceof interface native new package private protected public return static strictfp super " +
                "switch synchronized this throw throws transient try volatile while var record sealed permits non-sealed yield",
        ),
        types = words("boolean byte char double float int long short void"),
        strings = listOf(StringRule("\"\"\"", multiline = true), StringRule("\""), StringRule("'")),
        annotationPrefix = '@',
        identifierExtra = "$",
        capitalizedTypes = true,
    )

    val groovy = ProgramRules(
        keywords = java.keywords + words("def as in trait it"),
        types = java.types,
        strings = listOf(
            StringRule("\"\"\"", multiline = true, templates = StringTemplates.DOLLAR),
            StringRule("'''", multiline = true),
            StringRule("\"", templates = StringTemplates.DOLLAR),
            StringRule("'"),
        ),
        annotationPrefix = '@',
        identifierExtra = "$",
        capitalizedTypes = true,
    )

    val dart = ProgramRules(
        keywords = words(
            "abstract as assert async await base break case catch class const continue covariant default deferred do dynamic " +
                "else enum export extends extension external factory final finally for get hide if implements import in interface " +
                "is late library mixin new of on operator part required rethrow return sealed set show static super switch sync " +
                "this throw try typedef var when while with yield",
        ),
        types = words("void int double num bool"),
        strings = listOf(
            StringRule("\"\"\"", multiline = true, templates = StringTemplates.DOLLAR),
            StringRule("'''", multiline = true, templates = StringTemplates.DOLLAR),
            StringRule("\"", templates = StringTemplates.DOLLAR),
            StringRule("'", templates = StringTemplates.DOLLAR),
        ),
        stringPrefixes = setOf("r"),
        rawStringPrefixes = setOf("r"),
        annotationPrefix = '@',
        identifierExtra = "$",
        capitalizedTypes = true,
    )
}

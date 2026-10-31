package ru.aokazantsev.devatlas.themeformat.code.lexer

internal object ScriptFamilyRules {

    private val javaScriptKeywords = words(
        "async await break case catch class const continue debugger default delete do else export extends finally for from " +
            "function get if import in instanceof let new of return set static super switch this throw try typeof var void while " +
            "with yield",
    )
    private val javaScriptStrings = listOf(
        StringRule("`", multiline = true, templates = StringTemplates.DOLLAR_BRACE),
        StringRule("\""),
        StringRule("'"),
    )

    val javaScript = ProgramRules(
        keywords = javaScriptKeywords,
        literals = words("true false null undefined NaN Infinity"),
        strings = javaScriptStrings,
        identifierExtra = "$",
        regexLiterals = true,
        capitalizedTypes = true,
    )

    val typeScript = javaScript.copy(
        keywords = javaScriptKeywords + words(
            "abstract as asserts declare enum implements infer interface is keyof module namespace override private protected " +
                "public readonly satisfies type unique",
        ),
        types = words("any bigint boolean never number object string symbol unknown void"),
        annotationPrefix = '@',
    )

    val python = ProgramRules(
        keywords = words(
            "and as assert async await break class continue def del elif else except finally for from global if import in is " +
                "lambda nonlocal not or pass raise return try while with yield match case self",
        ),
        types = words("int float str bool bytes list dict set tuple object"),
        literals = words("True False None"),
        lineComments = listOf("#"),
        blockComment = null,
        strings = listOf(
            StringRule("\"\"\"", multiline = true),
            StringRule("'''", multiline = true),
            StringRule("\""),
            StringRule("'"),
        ),
        stringPrefixes = words("r u b f br rb fr rf"),
        annotationPrefix = '@',
    )

    val ruby = ProgramRules(
        keywords = words(
            "alias and begin break case class def defined? do else elsif end ensure for if in module next not or redo rescue " +
                "retry return self super then undef unless until when while yield require require_relative attr_accessor " +
                "attr_reader attr_writer private protected public lambda proc puts",
        ),
        literals = words("true false nil"),
        lineComments = listOf("#"),
        blockComment = BlockComment("=begin", "=end", atLineStart = true),
        strings = listOf(StringRule("\"", multiline = true), StringRule("'", multiline = true)),
        variablePrefixes = "@$",
        symbols = true,
        percentLiterals = true,
        heredocs = HeredocStyle.RUBY,
        regexLiterals = true,
        capitalizedTypes = true,
    )

    val php = ProgramRules(
        keywords = words(
            "abstract and array as break callable case catch class clone const continue declare default do echo else elseif " +
                "empty enddeclare endfor endforeach endif endswitch endwhile enum extends final finally fn for foreach function " +
                "global goto if implements include include_once instanceof insteadof interface isset list match namespace new or " +
                "print private protected public readonly require require_once return static switch throw trait try unset use " +
                "var while xor yield",
        ),
        types = words("int float bool string void mixed never iterable object self parent"),
        literals = words("true false null TRUE FALSE NULL"),
        lineComments = listOf("//", "#"),
        strings = listOf(StringRule("\"", multiline = true, templates = StringTemplates.DOLLAR), StringRule("'", multiline = true)),
        variablePrefixes = "$",
        hashAttributes = true,
        markers = listOf("<?php", "<?=", "?>"),
        markupAfter = "?>",
        capitalizedTypes = true,
    )

    val shell = ProgramRules(
        keywords = words(
            "if then else elif fi case esac for select while until do done in function time return exit break continue export " +
                "local readonly declare unset source alias shift set trap eval exec",
        ),
        literals = words("true false"),
        lineComments = listOf("#"),
        blockComment = null,
        strings = listOf(
            StringRule("\"", multiline = true, templates = StringTemplates.DOLLAR),
            StringRule("'", escapes = false, multiline = true),
        ),
        variablePrefixes = "$",
        commentAfterSpaceOnly = true,
        escapesOutsideStrings = true,
        heredocs = HeredocStyle.SHELL,
    )

    val sql = ProgramRules(
        keywords = words(
            "add all alter and any as asc begin between by case cascade check column commit constraint create cross database " +
                "default delete desc distinct drop else end exists foreign from full group having if in index inner insert into " +
                "is join key left like limit not null offset on or order outer primary references replace returning right " +
                "rollback select set table then transaction trigger truncate union unique update using values view when where " +
                "with",
        ),
        types = words(
            "int integer smallint bigint tinyint decimal numeric real float double boolean bool char varchar text blob date " +
                "time timestamp datetime serial uuid json jsonb",
        ),
        literals = words("true false"),
        lineComments = listOf("--"),
        strings = listOf(
            StringRule("'", escapes = false, multiline = true),
            StringRule("\"", escapes = false),
            StringRule("`", escapes = false),
        ),
        ignoreCase = true,
    )
}

package ru.aokazantsev.devatlas.themeformat.code

enum class CodeLanguage(val id: String, val title: String, val aliases: Set<String> = emptySet()) {
    KOTLIN("kotlin", "Kotlin", setOf("kt", "kts")),
    JAVA("java", "Java"),
    SWIFT("swift", "Swift"),
    OBJECTIVE_C("objectivec", "Objective-C", setOf("objc", "objective-c")),
    DART("dart", "Dart"),
    JAVASCRIPT("javascript", "JavaScript", setOf("js")),
    TYPESCRIPT("typescript", "TypeScript", setOf("ts")),
    PYTHON("python", "Python", setOf("py")),
    GO("go", "Go", setOf("golang")),
    RUST("rust", "Rust", setOf("rs")),
    C("c", "C"),
    CPP("cpp", "C++", setOf("c++")),
    CSHARP("csharp", "C#", setOf("cs")),
    PHP("php", "PHP"),
    RUBY("ruby", "Ruby", setOf("rb")),
    SHELL("shell", "Shell", setOf("sh", "bash")),
    SQL("sql", "SQL"),
    GROOVY("groovy", "Groovy", setOf("gradle")),
    JSON("json", "JSON"),
    YAML("yaml", "YAML", setOf("yml")),
    XML("xml", "XML"),
    HTML("html", "HTML"),
    CSS("css", "CSS"),
    MARKDOWN("markdown", "Markdown", setOf("md")),
    DIFF("diff", "Diff", setOf("patch")),
    TEXT("text", "Текст", setOf("plaintext", "txt"));

    companion object {
        private val byName: Map<String, CodeLanguage> =
            entries.flatMap { language -> (language.aliases + language.id).map { it to language } }.toMap()

        fun resolve(name: String): CodeLanguage? = byName[name.lowercase()]
    }
}

package ru.aokazantsev.devatlas.themeformat.issue

enum class HelpSection(val anchor: String) {
    FOLDER("folder"),
    ENCODING("encoding"),
    MANIFEST("manifest"),
    IDS("ids"),
    TREE("tree"),
    HTML("html"),
    CODE("code"),
    LINKS("links"),
    PDF("pdf"),
    IMAGES("images"),
    SVG("svg"),
    COLORS("colors"),
    ARCHIVE("archive");

    val url: String
        get() = "$HELP_PAGE_URL#$anchor"

    companion object {
        const val HELP_PAGE_URL = "https://aokazantsev.ru/pets/devatlas/themes/"
    }
}

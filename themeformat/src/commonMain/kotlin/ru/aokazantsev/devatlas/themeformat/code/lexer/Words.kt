package ru.aokazantsev.devatlas.themeformat.code.lexer

internal fun words(list: String): Set<String> = list.split(' ').filter { it.isNotEmpty() }.toSet()

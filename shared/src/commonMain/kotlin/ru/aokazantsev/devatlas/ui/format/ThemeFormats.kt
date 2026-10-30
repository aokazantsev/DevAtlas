package ru.aokazantsev.devatlas.ui.format

import ru.aokazantsev.devatlas.text.russianPlural

private const val KILOBYTE = 1024L
private const val MEGABYTE = KILOBYTE * KILOBYTE
private const val TENTHS = 10L

fun formatSize(bytes: Long): String = when {
    bytes >= MEGABYTE -> "${tenths(bytes, MEGABYTE)} МБ"
    bytes >= KILOBYTE -> "${(bytes + KILOBYTE - 1) / KILOBYTE} КБ"
    else -> "$bytes Б"
}

fun pagesText(count: Int): String = "$count ${russianPlural(count, one = "страница", few = "страницы", many = "страниц")}"

private fun tenths(bytes: Long, unit: Long): String {
    val scaled = bytes * TENTHS / unit
    return "${scaled / TENTHS},${scaled % TENTHS}"
}

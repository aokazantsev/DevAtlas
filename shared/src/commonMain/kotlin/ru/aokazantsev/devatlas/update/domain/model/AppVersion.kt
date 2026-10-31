package ru.aokazantsev.devatlas.update.domain.model

data class AppVersion(val parts: List<Int>) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        for (index in 0 until maxOf(parts.size, other.parts.size)) {
            val difference = parts.getOrElse(index) { 0 } - other.parts.getOrElse(index) { 0 }
            if (difference != 0) return difference
        }
        return 0
    }

    override fun toString(): String = parts.joinToString(".")

    companion object {
        private val VERSION = Regex("v?(\\d{1,6}(?:\\.\\d{1,6}){0,3})", RegexOption.IGNORE_CASE)

        fun parse(text: String): AppVersion? {
            val match = VERSION.matchEntire(text.trim().substringBefore('-')) ?: return null
            return AppVersion(match.groupValues[1].split('.').map(String::toInt))
        }
    }
}

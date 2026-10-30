package ru.aokazantsev.devatlas.themecheck

data class ThemeCheckOptions(
    val input: String,
    val zip: String?,
    val strict: Boolean,
) {
    companion object {
        fun parse(args: List<String>): ThemeCheckOptions? {
            var input: String? = null
            var zip: String? = null
            var strict = false
            var index = 0
            while (index < args.size) {
                when (val arg = args[index]) {
                    "--zip" -> zip = args.getOrNull(++index) ?: return null
                    "--strict" -> strict = true
                    else -> if (arg.startsWith("--") || input != null) return null else input = arg
                }
                index++
            }
            return input?.let { ThemeCheckOptions(it, zip, strict) }
        }
    }
}

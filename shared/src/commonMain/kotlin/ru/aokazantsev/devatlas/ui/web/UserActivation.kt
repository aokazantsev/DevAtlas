package ru.aokazantsev.devatlas.ui.web

internal class UserActivation(private val now: () -> Long) {
    private var grantedAt: Long? = null
    private val targets = mutableSetOf<String>()

    fun onUserInput() {
        grantedAt = now()
        targets.clear()
    }

    fun onNavigation(url: String, hasGesture: Boolean, isRedirect: Boolean) {
        if (!isActive()) return
        when {
            isRedirect -> if (targets.isNotEmpty()) targets += keyOf(url)
            hasGesture -> if (targets.isEmpty()) targets += keyOf(url)
        }
    }

    fun consume(url: String): Boolean {
        if (!isActive()) {
            clear()
            return false
        }
        if (targets.isNotEmpty() && keyOf(url) !in targets) return false
        clear()
        return true
    }

    fun onPageStarted() = clear()

    private fun isActive(): Boolean = grantedAt?.let { now() - it <= WINDOW_MILLIS } == true

    private fun clear() {
        grantedAt = null
        targets.clear()
    }

    private fun keyOf(url: String): String = url.substringBefore('#')

    companion object {
        const val WINDOW_MILLIS = 60_000L
    }
}

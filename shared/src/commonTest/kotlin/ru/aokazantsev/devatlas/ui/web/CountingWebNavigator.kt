package ru.aokazantsev.devatlas.ui.web

class CountingWebNavigator : WebNavigator {
    var reloads = 0
        private set

    override fun goBack() = Unit

    override fun reload() {
        reloads++
    }
}

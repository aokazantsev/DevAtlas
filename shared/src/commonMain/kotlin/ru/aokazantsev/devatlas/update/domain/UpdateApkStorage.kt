package ru.aokazantsev.devatlas.update.domain

interface UpdateApkStorage {
    fun discardAll()

    fun discardInstalled()
}

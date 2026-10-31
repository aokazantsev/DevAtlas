package ru.aokazantsev.devatlas.testing

import ru.aokazantsev.devatlas.update.domain.UpdateApkStorage

class FakeUpdateApkStorage : UpdateApkStorage {
    var discardedAll = 0
    var discardedInstalled = 0

    override fun discardAll() {
        discardedAll++
    }

    override fun discardInstalled() {
        discardedInstalled++
    }
}

package ru.aokazantsev.devatlas.e2e

import ru.aokazantsev.devatlas.DevAtlasApplication
import ru.aokazantsev.devatlas.di.NetworkEndpoints

class TestDevAtlasApplication : DevAtlasApplication() {

    override val holdsSystemSplash: Boolean = false

    override fun endpoints(): NetworkEndpoints = TestNetwork.endpoints()
}

package ru.aokazantsev.devatlas.startup.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ru.aokazantsev.devatlas.startup.domain.VerifiedManifests
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeManifest

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
internal class VerifiedManifestsImpl : VerifiedManifests {

    private var manifests: Map<String, ThemeManifest> = emptyMap()

    override fun replace(manifests: Map<String, ThemeManifest>) {
        this.manifests = manifests
    }

    override fun find(themeId: String): ThemeManifest? = manifests[themeId]

    override fun forget(themeId: String) {
        manifests = manifests - themeId
    }
}

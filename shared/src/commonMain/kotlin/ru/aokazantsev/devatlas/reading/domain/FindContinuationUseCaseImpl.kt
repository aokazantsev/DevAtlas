package ru.aokazantsev.devatlas.reading.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import ru.aokazantsev.devatlas.reading.domain.model.Continuation
import ru.aokazantsev.devatlas.settings.domain.ReadingSettingsRepository
import ru.aokazantsev.devatlas.startup.domain.VerifiedManifests

@ContributesBinding(AppScope::class)
@Inject
internal class FindContinuationUseCaseImpl(
    private val readingSettings: ReadingSettingsRepository,
    private val readingPoints: ReadingPointRepository,
    private val verifiedManifests: VerifiedManifests,
) : FindContinuationUseCase {

    override suspend fun invoke(themeId: String): Continuation? {
        if (!readingSettings.continueFromLastPoint.first()) return null
        val point = readingPoints.last(themeId) ?: return null
        val manifest = verifiedManifests.find(themeId) ?: return null
        return Continuation(manifest.tree, point)
    }
}

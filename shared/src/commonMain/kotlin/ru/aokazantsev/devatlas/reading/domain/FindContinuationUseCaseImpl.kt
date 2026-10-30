package ru.aokazantsev.devatlas.reading.domain

import androidx.sqlite.SQLiteException
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCase
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents
import ru.aokazantsev.devatlas.reading.domain.model.Continuation
import ru.aokazantsev.devatlas.settings.domain.ReadingSettingsRepository

@ContributesBinding(AppScope::class)
@Inject
internal class FindContinuationUseCaseImpl(
    private val readingSettings: ReadingSettingsRepository,
    private val readingPoints: ReadingPointRepository,
    private val observeContents: ObserveContentsUseCase,
) : FindContinuationUseCase {

    override suspend fun invoke(themeId: String): Continuation? {
        if (!readingSettings.continueFromLastPoint.first()) return null
        val point = readingPoints.last(themeId) ?: return null
        val contents = try {
            observeContents().first()
        } catch (_: SQLiteException) {
            return null
        }
        if (contents !is ThemeContents.Ready || contents.theme.id != themeId) return null
        return Continuation(contents.manifest.tree, point)
    }
}

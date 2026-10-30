package ru.aokazantsev.devatlas.contents.domain

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents
import ru.aokazantsev.devatlas.themes.domain.ProgressRepository
import ru.aokazantsev.devatlas.themes.domain.ThemeLibrary
import ru.aokazantsev.devatlas.themes.domain.ThemePackageReader
import ru.aokazantsev.devatlas.themes.domain.ThemeStorage

@ContributesBinding(AppScope::class)
@Inject
internal class ObserveContentsUseCaseImpl(
    private val library: ThemeLibrary,
    private val storage: ThemeStorage,
    private val reader: ThemePackageReader,
    private val progress: ProgressRepository,
) : ObserveContentsUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<ThemeContents> = library.observeCurrent()
        .distinctUntilChanged { old, new -> old?.id == new?.id && old?.sha256 == new?.sha256 }
        .flatMapLatest { theme ->
            if (theme == null) return@flatMapLatest flowOf(ThemeContents.NoTheme)
            val manifest = withContext(Dispatchers.IO) { reader.manifest(storage.themeDirectory(theme.id)) }
            if (manifest == null) {
                flowOf(ThemeContents.Unreadable)
            } else {
                progress.observeViewed(theme.id).map { viewed -> ThemeContents.Ready(theme, manifest, viewed) }
            }
        }
}

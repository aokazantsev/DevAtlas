package ru.aokazantsev.devatlas.themes.domain

import okio.Path
import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.DownloadResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan

interface ThemeDownloader {
    suspend fun download(plan: ThemeSourcePlan, validators: CacheValidators?, target: Path): DownloadResult
}

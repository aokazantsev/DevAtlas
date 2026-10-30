package ru.aokazantsev.devatlas.themes.domain

import ru.aokazantsev.devatlas.themes.domain.model.CacheValidators
import ru.aokazantsev.devatlas.themes.domain.model.FetchResult
import ru.aokazantsev.devatlas.themes.domain.model.ThemeSourcePlan

interface ThemePackageFetcher {
    suspend fun fetch(plan: ThemeSourcePlan, validators: CacheValidators? = null): FetchResult
}

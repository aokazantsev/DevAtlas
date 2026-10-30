package ru.aokazantsev.devatlas.themes.domain.model

data class CacheValidators(
    val etag: String?,
    val lastModified: String?,
) {
    val isEmpty: Boolean
        get() = etag == null && lastModified == null
}

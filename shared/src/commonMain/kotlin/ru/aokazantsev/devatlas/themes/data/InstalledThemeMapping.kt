package ru.aokazantsev.devatlas.themes.data

import ru.aokazantsev.devatlas.database.data.entity.InstalledThemeEntity
import ru.aokazantsev.devatlas.themes.domain.model.InstalledTheme

internal fun InstalledThemeEntity.toDomain() = InstalledTheme(
    id = id,
    title = title,
    description = description,
    sourceUrl = sourceUrl,
    etag = etag,
    lastModified = lastModified,
    sha256 = sha256,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
    lastUsedAt = lastUsedAt,
)

internal fun InstalledTheme.toEntity() = InstalledThemeEntity(
    id = id,
    title = title,
    description = description,
    sourceUrl = sourceUrl,
    etag = etag,
    lastModified = lastModified,
    sha256 = sha256,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
    lastUsedAt = lastUsedAt,
)

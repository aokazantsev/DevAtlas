package ru.aokazantsev.devatlas.update.domain.model

data class AppRelease(
    val version: AppVersion,
    val bodyHtml: String,
    val apk: ReleaseApk?,
)

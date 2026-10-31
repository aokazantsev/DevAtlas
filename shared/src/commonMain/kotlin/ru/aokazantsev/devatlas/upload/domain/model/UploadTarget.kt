package ru.aokazantsev.devatlas.upload.domain.model

enum class UploadTarget(val journalName: String) {
    CRASH_REPORT("crash report"),
    USAGE("usage statistics"),
}

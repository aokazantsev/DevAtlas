package ru.aokazantsev.devatlas.crash.data

import ru.aokazantsev.devatlas.crash.domain.CrashReports

fun installCrashCatcher(reports: () -> CrashReports) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    if (previous is CrashCatcher) return
    Thread.setDefaultUncaughtExceptionHandler(CrashCatcher(reports, previous))
}

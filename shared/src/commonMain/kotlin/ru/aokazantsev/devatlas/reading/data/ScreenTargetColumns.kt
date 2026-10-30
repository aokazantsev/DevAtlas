package ru.aokazantsev.devatlas.reading.data

import ru.aokazantsev.devatlas.reading.domain.model.ScreenTarget

internal object ScreenTargetColumns {
    private const val ROOT = "root"
    private const val SECTION = "section"
    private const val PAGE = "page"

    fun kind(target: ScreenTarget): String = when (target) {
        ScreenTarget.Root -> ROOT
        is ScreenTarget.Section -> SECTION
        is ScreenTarget.Page -> PAGE
    }

    fun nodeId(target: ScreenTarget): String = when (target) {
        ScreenTarget.Root -> ""
        is ScreenTarget.Section -> target.id
        is ScreenTarget.Page -> target.id
    }

    fun target(kind: String, nodeId: String): ScreenTarget? = when (kind) {
        ROOT -> ScreenTarget.Root
        SECTION -> ScreenTarget.Section(nodeId)
        PAGE -> ScreenTarget.Page(nodeId)
        else -> null
    }
}

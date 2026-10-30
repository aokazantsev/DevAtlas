package ru.aokazantsev.devatlas.ui.contents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.aokazantsev.devatlas.contents.domain.ObserveContentsUseCase
import ru.aokazantsev.devatlas.contents.domain.model.ThemeContents
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode

@AssistedInject
class ContentsViewModel(
    @Assisted private val sectionId: String?,
    observeContents: ObserveContentsUseCase,
) : ViewModel() {

    val state: StateFlow<ContentsState> = observeContents()
        .map(::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ContentsState.Loading)

    private fun toState(contents: ThemeContents): ContentsState = when (contents) {
        ThemeContents.NoTheme -> ContentsState.NoTheme
        ThemeContents.Unreadable -> ContentsState.Unreadable
        is ThemeContents.Ready -> {
            val viewed = contents.viewedPageIds
            if (sectionId == null) {
                ContentsState.Ready(
                    title = contents.theme.title,
                    description = contents.theme.description,
                    progress = progressOf(contents.manifest.tree, viewed),
                    rows = contents.manifest.tree.map { it.toRow(viewed) },
                )
            } else {
                val section = contents.manifest.tree.findSection(sectionId)
                if (section == null) {
                    ContentsState.SectionMissing
                } else {
                    ContentsState.Ready(
                        title = section.title,
                        description = null,
                        progress = null,
                        rows = section.children.map { it.toRow(viewed) },
                    )
                }
            }
        }
    }

    private fun ThemeNode.toRow(viewed: Set<String>): ContentsRow = when (this) {
        is ThemeNode.Page -> ContentsRow.Page(id = id, title = title, viewed = id in viewed)
        is ThemeNode.Section -> ContentsRow.Section(id = id, title = title, progress = progressOf(children, viewed))
    }

    private fun progressOf(nodes: List<ThemeNode>, viewed: Set<String>): ContentsProgress {
        val pageIds = nodes.flatMap { it.pageIds() }
        return ContentsProgress(viewed = pageIds.count { it in viewed }, total = pageIds.size)
    }

    private fun ThemeNode.pageIds(): List<String> = when (this) {
        is ThemeNode.Page -> listOf(id)
        is ThemeNode.Section -> children.flatMap { it.pageIds() }
    }

    private fun List<ThemeNode>.findSection(id: String): ThemeNode.Section? {
        forEach { node ->
            if (node is ThemeNode.Section) {
                if (node.id == id) return node
                node.children.findSection(id)?.let { return it }
            }
        }
        return null
    }

    @AssistedFactory
    fun interface Factory {
        fun create(sectionId: String?): ContentsViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

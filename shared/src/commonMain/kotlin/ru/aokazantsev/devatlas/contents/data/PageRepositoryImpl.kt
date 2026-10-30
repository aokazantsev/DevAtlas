package ru.aokazantsev.devatlas.contents.data

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.IOException
import okio.Path
import ru.aokazantsev.devatlas.contents.domain.PageRepository
import ru.aokazantsev.devatlas.contents.domain.model.PageContent
import ru.aokazantsev.devatlas.themeformat.manifest.ThemeNode
import ru.aokazantsev.devatlas.themeformat.page.HtmlPageParser
import ru.aokazantsev.devatlas.themeformat.page.model.PageBlock
import ru.aokazantsev.devatlas.themeformat.path.ThemePaths
import ru.aokazantsev.devatlas.themeformat.svg.SvgRenderBudget
import ru.aokazantsev.devatlas.themes.domain.ThemeStorage

@ContributesBinding(AppScope::class)
@Inject
internal class PageRepositoryImpl(
    private val storage: ThemeStorage,
    private val fileSystem: FileSystem,
) : PageRepository {

    override suspend fun load(themeId: String, page: ThemeNode.Page): PageContent? = withContext(Dispatchers.IO) {
        val directory = storage.themeDirectory(themeId)
        val source = readText(directory, page.file) ?: return@withContext null
        val document = HtmlPageParser.parse(page.file, source).document
        val diagrams = document.blocks.figureSources()
            .mapNotNull { path -> readText(directory, path)?.takeIf(SvgRenderBudget::fits)?.let { path to it } }
            .toMap()
        PageContent(document = document, diagrams = diagrams)
    }

    private fun readText(directory: Path, relative: String): String? {
        if (!ThemePaths.isPlainRelative(relative)) return null
        return try {
            fileSystem.read(directory / relative) { readByteArray() }
                .decodeToString(throwOnInvalidSequence = true)
                .removePrefix(BYTE_ORDER_MARK)
        } catch (_: IOException) {
            null
        } catch (_: CharacterCodingException) {
            null
        }
    }

    private fun List<PageBlock>.figureSources(): Set<String> = buildSet {
        this@figureSources.forEach { block ->
            when (block) {
                is PageBlock.Figure -> add(block.source)
                is PageBlock.Quote -> addAll(block.blocks.figureSources())
                is PageBlock.ListBlock -> block.items.forEach { addAll(it.figureSources()) }
                else -> Unit
            }
        }
    }

    private companion object {
        val BYTE_ORDER_MARK = Char(0xFEFF).toString()
    }
}

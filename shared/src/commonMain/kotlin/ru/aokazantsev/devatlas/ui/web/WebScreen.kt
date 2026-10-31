package ru.aokazantsev.devatlas.ui.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasError
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasProgressBar
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasTextButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.ui.links.LinkActions

@Composable
fun WebScreen(url: String, startUrl: String, links: LinkActions, onBack: () -> Unit, onNothingShown: () -> Unit) {
    val state = remember { WebPageState() }
    val shownUrl = state.url?.takeIf { it != startUrl } ?: url
    AtlasScaffold(
        topBar = {
            AtlasTopBar(
                title = state.title ?: hostOf(shownUrl),
                onBack = { if (state.canGoBack) state.goBack() else onBack() },
                actions = {
                    AtlasIconButton(
                        icon = AtlasIcons.ContentCopy,
                        contentDescription = "Копировать ссылку",
                        onClick = { links.copyLink(shownUrl) },
                    )
                    AtlasIconButton(
                        icon = AtlasIcons.OpenInNew,
                        contentDescription = "Открыть в браузере",
                        onClick = { links.openExternal(shownUrl) },
                    )
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            WebPage(
                startUrl = startUrl,
                state = state,
                onPdf = links.openPdf,
                onExternal = links.openExternal,
                onNothingShown = onNothingShown,
                modifier = Modifier.fillMaxSize(),
            )
            state.progress?.let { progress -> AtlasProgressBar(progress = progress, modifier = Modifier.align(Alignment.TopCenter)) }
            if (state.failed) {
                AtlasError(
                    message = "Страница не открылась: нет сети или сайт недоступен",
                    actionText = "Повторить",
                    onAction = state::reload,
                    extraActions = { AtlasTextButton(text = "Открыть в браузере", onClick = { links.openExternal(shownUrl) }) },
                    modifier = Modifier.background(AtlasTheme.colors.surfaceContainer),
                )
            }
        }
    }
}

private fun hostOf(url: String): String = url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')

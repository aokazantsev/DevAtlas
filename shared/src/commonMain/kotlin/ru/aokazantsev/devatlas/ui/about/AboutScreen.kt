package ru.aokazantsev.devatlas.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.aokazantsev.devatlas.designsystem.component.AtlasListGroup
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasRowAction
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.di.AppLinks

@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenPolicy: () -> Unit,
) {
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    AtlasScaffold(topBar = { AtlasTopBar(title = "О приложении", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AtlasTheme.spacing.m),
        ) {
            AtlasListGroup {
                AtlasListRow(title = "Версия", subtitle = viewModel.versionName, leadingIcon = AtlasIcons.Info, index = 0, count = 2)
                AtlasListRow(
                    title = "Проверить обновление",
                    leadingIcon = AtlasIcons.SystemUpdate,
                    showChevron = true,
                    index = 1,
                    count = 2,
                    onClick = onCheckUpdate,
                )
            }
            AtlasListGroup {
                LinkRow(
                    title = "Сайт проекта",
                    url = AppLinks.SITE_URL,
                    icon = AtlasIcons.Public,
                    notice = notice,
                    viewModel = viewModel,
                    index = 0,
                )
                LinkRow(
                    title = "Исходный код на GitHub",
                    url = AppLinks.GITHUB_URL,
                    icon = AtlasIcons.Code,
                    notice = notice,
                    viewModel = viewModel,
                    index = 1,
                )
                AtlasListRow(
                    title = "Политика конфиденциальности",
                    subtitle = "Какие данные уходят и как их отключить",
                    leadingIcon = AtlasIcons.Policy,
                    showChevron = true,
                    index = 2,
                    count = LINK_ROWS,
                    onClick = onOpenPolicy,
                )
                AtlasListRow(
                    title = "Поделиться приложением",
                    subtitle = notice?.takeIf { it.key == AboutViewModel.SHARE_KEY }?.text,
                    leadingIcon = AtlasIcons.Share,
                    index = 3,
                    count = LINK_ROWS,
                    onClick = viewModel::share,
                )
            }
        }
    }
}

@Composable
private fun LinkRow(
    title: String,
    url: String,
    icon: ImageVector,
    notice: AboutNotice?,
    viewModel: AboutViewModel,
    index: Int,
) {
    AtlasListRow(
        title = title,
        subtitle = notice?.takeIf { it.key == url }?.text ?: url.removePrefix("https://").trimEnd('/'),
        leadingIcon = icon,
        index = index,
        count = LINK_ROWS,
        trailingAction = AtlasRowAction(icon = AtlasIcons.ContentCopy, contentDescription = "Копировать ссылку: $title") {
            viewModel.copy(url)
        },
        onClick = { viewModel.open(url) },
    )
}

private const val LINK_ROWS = 4

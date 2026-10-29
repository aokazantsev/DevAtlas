package ru.aokazantsev.devatlas.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.component.AtlasListRow
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar

@Composable
fun AboutScreen(versionName: String, onBack: () -> Unit) {
    AtlasScaffold(topBar = { AtlasTopBar(title = "О приложении", onBack = onBack) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            AtlasListRow(title = "Версия", subtitle = versionName)
        }
    }
}

package ru.aokazantsev.devatlas.ui.catalog

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.aokazantsev.devatlas.designsystem.catalog.ComponentCatalog
import ru.aokazantsev.devatlas.designsystem.component.AtlasScaffold
import ru.aokazantsev.devatlas.designsystem.component.AtlasTopBar

@Composable
fun ComponentCatalogScreen(onBack: () -> Unit) {
    AtlasScaffold(topBar = { AtlasTopBar(title = "Каталог компонентов", onBack = onBack) }) { padding ->
        ComponentCatalog(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        )
    }
}

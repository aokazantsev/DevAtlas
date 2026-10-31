package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun SvgImage(
    svg: String,
    palette: SvgPalette,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    loading: @Composable (Modifier) -> Unit,
    fallback: @Composable (Modifier) -> Unit,
)

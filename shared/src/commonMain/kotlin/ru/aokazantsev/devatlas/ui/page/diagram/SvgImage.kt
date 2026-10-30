package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SvgImage(
    svg: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
)

package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class AtlasSizes(
    val icon: Dp,
    val iconLarge: Dp,
    val loader: Dp,
    val swatch: Dp,
    val shapeSample: Dp,
) {
    companion object {
        val Default = AtlasSizes(
            icon = 24.dp,
            iconLarge = 48.dp,
            loader = 48.dp,
            swatch = 56.dp,
            shapeSample = 64.dp,
        )
    }
}

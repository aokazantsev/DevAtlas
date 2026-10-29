package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class AtlasSizes(
    val icon: Dp,
    val iconLarge: Dp,
    val touchTarget: Dp,
    val listRowMinHeight: Dp,
    val buttonHeight: Dp,
    val topBarHeight: Dp,
    val divider: Dp,
    val loader: Dp,
    val loaderStroke: Dp,
) {
    companion object {
        val Default = AtlasSizes(
            icon = 24.dp,
            iconLarge = 48.dp,
            touchTarget = 48.dp,
            listRowMinHeight = 56.dp,
            buttonHeight = 48.dp,
            topBarHeight = 56.dp,
            divider = 1.dp,
            loader = 40.dp,
            loaderStroke = 3.dp,
        )
    }
}

package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
class AtlasSpacing(
    val xxs: Dp,
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val xl: Dp,
) {
    companion object {
        val Default = AtlasSpacing(
            xxs = 2.dp,
            xs = 4.dp,
            s = 8.dp,
            m = 12.dp,
            l = 16.dp,
            xl = 24.dp,
        )
    }
}

package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
class AtlasTypography(
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val code: TextStyle,
) {
    companion object {
        val Default = AtlasTypography(
            title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            heading = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
            body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
            label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
            code = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = FontFamily.Monospace),
        )
    }
}

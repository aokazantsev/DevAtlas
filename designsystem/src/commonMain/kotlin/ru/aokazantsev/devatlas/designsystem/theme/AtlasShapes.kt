package ru.aokazantsev.devatlas.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
class AtlasShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val sheet: Shape,
) {
    companion object {
        val Default = AtlasShapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(20.dp),
            sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        )
    }
}

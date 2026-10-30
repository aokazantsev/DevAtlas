package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class AtlasChoiceOption<T>(
    val value: T,
    val title: String,
    val icon: ImageVector? = null,
)

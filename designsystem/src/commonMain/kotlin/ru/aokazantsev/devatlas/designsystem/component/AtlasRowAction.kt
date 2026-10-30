package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.ui.graphics.vector.ImageVector

data class AtlasRowAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)

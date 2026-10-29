package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.runtime.Immutable

@Immutable
data class AtlasCheckOption(
    val key: String,
    val title: String,
    val checked: Boolean,
)

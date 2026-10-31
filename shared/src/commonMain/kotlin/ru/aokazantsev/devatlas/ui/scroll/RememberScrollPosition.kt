package ru.aokazantsev.devatlas.ui.scroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun rememberScrollPosition(anchor: Int): ScrollPosition =
    rememberSaveable(saver = ScrollPosition.Saver) { ScrollPosition(anchor) }

package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasProgressBar(progress: Float?, modifier: Modifier = Modifier) {
    if (progress == null) {
        LinearWavyProgressIndicator(modifier = modifier.fillMaxWidth())
    } else {
        LinearWavyProgressIndicator(progress = { progress }, modifier = modifier.fillMaxWidth())
    }
}

package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.aokazantsev.devatlas.designsystem.component.AtlasIconButton
import ru.aokazantsev.devatlas.designsystem.component.AtlasLoadingIndicator
import ru.aokazantsev.devatlas.designsystem.icon.AtlasIcons
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

const val DIAGRAM_VIEWER_TAG = "diagram_viewer"

@Composable
internal fun DiagramViewer(svg: String, contentDescription: String?, onClose: () -> Unit) {
    val palette = rememberSvgPalette()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var scale by rememberSaveable { mutableFloatStateOf(1f) }
        var offset by rememberSaveable(stateSaver = OffsetSaver) { mutableStateOf(Offset.Zero) }
        var viewport by remember { mutableStateOf(IntSize.Zero) }
        var diagram by remember { mutableStateOf(IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AtlasTheme.colors.surface)
                .semantics { testTag = DIAGRAM_VIEWER_TAG }
                .onSizeChanged {
                    viewport = it
                    if (diagram != IntSize.Zero) offset = offset.limitedTo(it, diagram, scale)
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                        offset = offset.zoomedAt(centroid, viewport, newScale / scale).plus(pan).limitedTo(viewport, diagram, newScale)
                        scale = newScale
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            SvgImage(
                svg = svg,
                palette = palette,
                contentDescription = contentDescription,
                modifier = Modifier
                    .onSizeChanged {
                        diagram = it
                        if (viewport != IntSize.Zero) offset = offset.limitedTo(viewport, it, scale)
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                loading = { AtlasLoadingIndicator() },
                fallback = { DiagramUnavailable() },
            )
            ViewerCloseButton(onClose = onClose, modifier = Modifier.align(Alignment.TopStart))
        }
    }
}

@Composable
private fun ViewerCloseButton(onClose: () -> Unit, modifier: Modifier) {
    AtlasIconButton(
        icon = AtlasIcons.Close,
        contentDescription = "Закрыть",
        onClick = onClose,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(AtlasTheme.spacing.s)
            .background(AtlasTheme.colors.surfaceContainerHigh, AtlasTheme.shapes.full),
    )
}

internal fun Offset.zoomedAt(centroid: Offset, viewport: IntSize, factor: Float): Offset {
    val fromCenter = centroid - Offset(viewport.width / 2f, viewport.height / 2f)
    return this * factor + fromCenter * (1 - factor)
}

internal fun Offset.limitedTo(viewport: IntSize, diagram: IntSize, scale: Float): Offset {
    val limitX = ((diagram.width * scale - viewport.width) / 2).coerceAtLeast(0f)
    val limitY = ((diagram.height * scale - viewport.height) / 2).coerceAtLeast(0f)
    return Offset(x.coerceIn(-limitX, limitX), y.coerceIn(-limitY, limitY))
}

private val OffsetSaver = Saver<Offset, List<Float>>(
    save = { listOf(it.x, it.y) },
    restore = { Offset(it[0], it[1]) },
)

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 8f

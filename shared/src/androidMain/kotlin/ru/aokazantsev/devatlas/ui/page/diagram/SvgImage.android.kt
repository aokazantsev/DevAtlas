package ru.aokazantsev.devatlas.ui.page.diagram

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun SvgImage(
    svg: String,
    palette: SvgPalette,
    contentDescription: String?,
    modifier: Modifier,
    loading: @Composable () -> Unit,
    fallback: @Composable () -> Unit,
) {
    val parsed by produceState<ParsedSvg?>(initialValue = null, svg, palette) {
        value = withContext(Dispatchers.Default) {
            ParsedSvg(parse(SvgColorTokens.resolve(svg, palette.roles, palette.textColor)))
        }
    }
    val ready = parsed
    if (ready == null) {
        loading()
        return
    }
    val document = ready.document
    if (document == null) {
        fallback()
        return
    }
    val box = document.documentViewBox
    Canvas(
        modifier = modifier
            .aspectRatio((box.width() / box.height()).coerceAtLeast(MIN_ASPECT_RATIO))
            .semantics {
                role = Role.Image
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        drawIntoCanvas { canvas ->
            document.renderToCanvas(canvas.nativeCanvas, RenderOptions.create().viewPort(0f, 0f, size.width, size.height))
        }
    }
}

private fun parse(svg: String): SVG? {
    val document = try {
        SVG.getFromString(svg)
    } catch (_: SVGParseException) {
        return null
    }
    val box = document.documentViewBox ?: sizeBox(document)
    val ratio = box.width() / box.height()
    if (!ratio.isFinite() || ratio <= 0f) return null
    if (ratio < MIN_ASPECT_RATIO) document.documentPreserveAspectRatio = PreserveAspectRatio.LETTERBOX
    document.setDocumentViewBox(box.left, box.top, box.width(), box.height())
    document.setDocumentWidth("100%")
    document.setDocumentHeight("100%")
    return document
}

private fun sizeBox(document: SVG): RectF {
    val width = document.documentWidth
    val height = document.documentHeight
    return if (width > 0f && height > 0f) RectF(0f, 0f, width, height) else RectF(0f, 0f, DEFAULT_WIDTH, DEFAULT_HEIGHT)
}

private const val MIN_ASPECT_RATIO = 0.25f
private const val DEFAULT_WIDTH = 300f
private const val DEFAULT_HEIGHT = 150f

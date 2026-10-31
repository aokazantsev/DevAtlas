package ru.aokazantsev.devatlas.ui.page.diagram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import ru.aokazantsev.devatlas.themeformat.svg.SvgViewport

@Composable
internal actual fun SvgImage(
    svg: String,
    palette: SvgPalette,
    contentDescription: String?,
    modifier: Modifier,
    loading: @Composable (Modifier) -> Unit,
    fallback: @Composable (Modifier) -> Unit,
) {
    val viewport = remember(svg) { SvgViewport.of(svg) }
    if (viewport == null) {
        fallback(Modifier)
        return
    }
    val sized = Modifier.aspectRatio(viewport.ratio.coerceAtLeast(MIN_ASPECT_RATIO))
    val parsed by produceState<ParsedSvg?>(initialValue = null, svg, palette, viewport) {
        value = withContext(Dispatchers.Default) {
            ParsedSvg(parse(SvgColorTokens.resolve(svg, palette.roles, palette.textColor), viewport))
        }
    }
    val ready = parsed
    if (ready == null) {
        loading(sized)
        return
    }
    val document = ready.document
    if (document == null) {
        fallback(Modifier.aspectRatio(viewport.ratio.coerceAtLeast(1f)))
        return
    }
    Canvas(
        modifier = modifier
            .then(sized)
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

private fun parse(svg: String, viewport: SvgViewport): SVG? {
    val document = try {
        SVG.getFromString(svg)
    } catch (_: SVGParseException) {
        return null
    }
    if (viewport.ratio < MIN_ASPECT_RATIO) document.documentPreserveAspectRatio = PreserveAspectRatio.LETTERBOX
    document.setDocumentViewBox(viewport.left, viewport.top, viewport.width, viewport.height)
    document.setDocumentWidth("100%")
    document.setDocumentHeight("100%")
    return document
}

private const val MIN_ASPECT_RATIO = 0.25f

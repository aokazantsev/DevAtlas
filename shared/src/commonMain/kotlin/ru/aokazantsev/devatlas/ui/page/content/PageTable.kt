package ru.aokazantsev.devatlas.ui.page.content

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme
import ru.aokazantsev.devatlas.themeformat.page.model.TableCell
import ru.aokazantsev.devatlas.themeformat.page.model.TableRow

@Composable
internal fun PageTable(rows: List<TableRow>, links: PageLinks, modifier: Modifier = Modifier) {
    val columns = rows.maxOfOrNull { it.cells.size } ?: 0
    if (columns == 0) return
    val columnMax = AtlasTheme.sizes.tableColumnMax
    Box(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Layout(
            modifier = Modifier.clip(AtlasTheme.shapes.medium).background(AtlasTheme.colors.surfaceContainerLow),
            content = {
                rows.forEach { row ->
                    repeat(columns) { column -> TableCellBox(row.cells.getOrNull(column), links) }
                }
            },
        ) { measurables, _ ->
            val maxWidth = columnMax.roundToPx()
            val widths = IntArray(columns) { column ->
                rows.indices.maxOf { row -> measurables[row * columns + column].maxIntrinsicWidth(Constraints.Infinity) }
                    .coerceAtMost(maxWidth)
            }
            val heights = IntArray(rows.size) { row ->
                (0 until columns).maxOf { column -> measurables[row * columns + column].minIntrinsicHeight(widths[column]) }
            }
            val placeables = measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixed(widths[index % columns], heights[index / columns]))
            }
            layout(widths.sum(), heights.sum()) {
                var y = 0
                rows.indices.forEach { row ->
                    var x = 0
                    repeat(columns) { column ->
                        placeables[row * columns + column].place(x, y)
                        x += widths[column]
                    }
                    y += heights[row]
                }
            }
        }
    }
}

@Composable
private fun TableCellBox(cell: TableCell?, links: PageLinks) {
    val header = cell?.header == true
    val background = if (header) AtlasTheme.colors.surfaceContainerHigh else AtlasTheme.colors.surfaceContainerLow
    Box(modifier = Modifier.fillMaxSize().background(background).padding(AtlasTheme.spacing.s)) {
        if (cell != null) {
            val style = AtlasTheme.typography.bodyMedium
            InlineText(
                content = cell.content,
                links = links,
                style = if (header) style.copy(fontWeight = FontWeight.Bold) else style,
            )
        }
    }
}

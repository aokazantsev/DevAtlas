package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AtlasTheme.typography.body,
    color: Color = AtlasTheme.colors.onSurface,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

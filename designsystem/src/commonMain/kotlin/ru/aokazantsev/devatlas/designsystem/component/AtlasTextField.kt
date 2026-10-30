package ru.aokazantsev.devatlas.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import ru.aokazantsev.devatlas.designsystem.theme.AtlasTheme

@Composable
fun AtlasTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Uri,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { AtlasText(text = label, maxLines = 1) },
        supportingText = supportingText?.let { { AtlasText(text = it) } },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = AtlasTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AtlasTheme.colors.surface,
            unfocusedContainerColor = AtlasTheme.colors.surface,
            errorContainerColor = AtlasTheme.colors.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        ),
    )
}

package ru.aokazantsev.devatlas.detekt

import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class DevAtlasRulesTest {

    @Test
    fun `compose color constructor and constants are reported`() {
        val findings = ColorOutsideDesignSystem().lint(
            """
            import androidx.compose.ui.graphics.Color

            val accent = Color(0xFF00FF00)
            val white = Color.White
            val hsv = Color.hsv(0f, 0f, 0f)
            val none = Color.Unspecified
            val clear = Color.Transparent
            val token = colors.accent
            """.trimIndent(),
        )

        assertEquals(listOf(3, 4, 5), findings.map { it.location.source.line })
    }

    @Test
    fun `aliased and fully qualified compose colors are reported`() {
        val findings = ColorOutsideDesignSystem().lint(
            """
            import androidx.compose.ui.graphics.Color as ComposeColor

            val a = ComposeColor(0xFF000000)
            val b = ComposeColor.Black
            val c = androidx.compose.ui.graphics.Color(0xFF000000)
            val d = androidx.compose.ui.graphics.Color.Red
            """.trimIndent(),
        )

        assertEquals(listOf(3, 4, 5, 6), findings.map { it.location.source.line })
    }

    @Test
    fun `allowed members are not reported and qualified constructor is reported once`() {
        val allowed = ColorOutsideDesignSystem().lint(
            """
            import androidx.compose.ui.graphics.Color as ComposeColor

            val a = ComposeColor.Transparent
            val b = androidx.compose.ui.graphics.Color.Unspecified
            """.trimIndent(),
        )
        val qualified = ColorOutsideDesignSystem().lint(
            """
            import androidx.compose.ui.graphics.Color

            val c = androidx.compose.ui.graphics.Color(0xFF000000)
            """.trimIndent(),
        )

        assertEquals(0, allowed.size)
        assertEquals(listOf(3), qualified.map { it.location.source.line })
    }

    @Test
    fun `android graphics color is not reported`() {
        val findings = ColorOutsideDesignSystem().lint(
            """
            import android.graphics.Color

            val transparent = Color.TRANSPARENT
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `dp literals are reported and tokens are not`() {
        val findings = DpLiteralOutsideDesignSystem().lint(
            """
            val a = 16.dp
            val b = 0.5.dp
            val c = -4.dp
            val d = (8).dp
            val e = Dp(24f)
            val f = (-4).dp
            val g = Dp(-4f)
            val h = Dp((24f))
            val i = Dp(value = 24f)
            val j = spacing.m
            val k = size.dp
            val l = Dp(size)
            """.trimIndent(),
        )

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), findings.map { it.location.source.line })
    }

    @Test
    fun `material3 import and fully qualified usage are reported`() {
        val findings = Material3OutsideDesignSystem().lint(
            """
            package ru.example

            import androidx.compose.material3.Text
            import androidx.compose.foundation.layout.Column

            val shape = androidx.compose.material3.MaterialTheme.shapes
            """.trimIndent(),
        )

        assertEquals(listOf(3, 6), findings.map { it.location.source.line })
    }
}

package ru.aokazantsev.devatlas.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile

class ColorOutsideDesignSystem(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Цвет задаётся только токеном дизайн-системы: AtlasTheme.colors.",
        Debt.FIVE_MINS,
    )

    private var composeColorNames: Set<String> = emptySet()

    override fun visitKtFile(file: KtFile) {
        composeColorNames = file.importDirectives
            .filter { it.importPath?.pathStr == COMPOSE_COLOR }
            .map { it.aliasName ?: COLOR }
            .toSet()
        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text
        val isQualified = (expression.parent as? KtDotQualifiedExpression)?.selectorExpression == expression
        if (callee in composeColorNames && !isQualified) {
            report(CodeSmell(issue, Entity.from(expression), "Color(...) вне :designsystem: возьми токен из AtlasTheme.colors."))
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val receiver = expression.receiverExpression.text
        val member = expression.selectorExpression?.text?.substringBefore('(')
        val isComposeColorMember = (receiver in composeColorNames || receiver == COMPOSE_COLOR) && member !in ALLOWED_MEMBERS
        val isQualifiedConstructor = receiver == COMPOSE_GRAPHICS && expression.selectorExpression is KtCallExpression && member == COLOR
        if (isComposeColorMember || isQualifiedConstructor) {
            report(CodeSmell(issue, Entity.from(expression), "${expression.text} вне :designsystem: возьми токен из AtlasTheme.colors."))
        }
    }

    private companion object {
        const val COLOR = "Color"
        const val COMPOSE_GRAPHICS = "androidx.compose.ui.graphics"
        const val COMPOSE_COLOR = "$COMPOSE_GRAPHICS.$COLOR"
        val ALLOWED_MEMBERS = setOf("Unspecified", "Transparent")
    }
}

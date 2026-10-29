package ru.aokazantsev.devatlas.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtPsiUtil

class DpLiteralOutsideDesignSystem(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Отступы и размеры задаются только токенами дизайн-системы: AtlasTheme.spacing и AtlasTheme.sizes.",
        Debt.FIVE_MINS,
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val isDp = expression.selectorExpression?.text == DP
        if (isDp && expression.receiverExpression.isNumberLiteral()) {
            report(CodeSmell(issue, Entity.from(expression), "Литерал ${expression.text} вне :designsystem: возьми токен."))
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val argument = expression.valueArguments.singleOrNull()?.getArgumentExpression()
        if (expression.calleeExpression?.text == DP_CONSTRUCTOR && argument?.isNumberLiteral() == true) {
            report(CodeSmell(issue, Entity.from(expression), "Литерал ${expression.text} вне :designsystem: возьми токен."))
        }
    }

    private fun KtExpression.isNumberLiteral(): Boolean {
        val unwrapped = KtPsiUtil.safeDeparenthesize(this)
        return unwrapped is KtConstantExpression ||
            unwrapped is KtPrefixExpression && unwrapped.baseExpression?.isNumberLiteral() == true
    }

    private companion object {
        const val DP = "dp"
        const val DP_CONSTRUCTOR = "Dp"
    }
}

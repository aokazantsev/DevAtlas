package ru.aokazantsev.devatlas.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class Material3OutsideDesignSystem(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Компоненты Material3 используются только внутри :designsystem.",
        Debt.TEN_MINS,
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val path = importDirective.importPath?.pathStr.orEmpty()
        if (path.isMaterial3()) {
            report(CodeSmell(issue, Entity.from(importDirective), "Импорт $path вне :designsystem: возьми компонент Atlas*."))
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val isTopmost = expression.parent !is KtDotQualifiedExpression
        val isDeclaration = expression.getStrictParentOfType<KtImportDirective>() != null ||
            expression.getStrictParentOfType<KtPackageDirective>() != null
        if (isTopmost && !isDeclaration && expression.text.isMaterial3()) {
            report(CodeSmell(issue, Entity.from(expression), "Material3 по полному имени вне :designsystem."))
        }
    }

    private fun String.isMaterial3(): Boolean = this == MATERIAL3_PACKAGE || startsWith("$MATERIAL3_PACKAGE.")

    private companion object {
        const val MATERIAL3_PACKAGE = "androidx.compose.material3"
    }
}

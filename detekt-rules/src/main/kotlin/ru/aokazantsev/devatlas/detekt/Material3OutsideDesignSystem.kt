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
        "Material3 и MaterialKolor используются только внутри :designsystem.",
        Debt.TEN_MINS,
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val path = importDirective.importPath?.pathStr.orEmpty()
        if (path.isDesignSystemLibrary()) {
            report(CodeSmell(issue, Entity.from(importDirective), "Импорт $path вне :designsystem: возьми компонент Atlas*."))
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val isTopmost = expression.parent !is KtDotQualifiedExpression
        val isDeclaration = expression.getStrictParentOfType<KtImportDirective>() != null ||
            expression.getStrictParentOfType<KtPackageDirective>() != null
        if (isTopmost && !isDeclaration && expression.text.isDesignSystemLibrary()) {
            report(CodeSmell(issue, Entity.from(expression), "Material3 или MaterialKolor по полному имени вне :designsystem."))
        }
    }

    private fun String.isDesignSystemLibrary(): Boolean = DESIGN_SYSTEM_PACKAGES.any { this == it || startsWith("$it.") }

    private companion object {
        val DESIGN_SYSTEM_PACKAGES = listOf("androidx.compose.material3", "com.materialkolor")
    }
}

package ru.aokazantsev.devatlas.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class DevAtlasRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "devatlas"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ColorOutsideDesignSystem(config),
            DpLiteralOutsideDesignSystem(config),
            Material3OutsideDesignSystem(config),
        ),
    )
}

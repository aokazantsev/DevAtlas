package ru.aokazantsev.devatlas.e2e

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.rules.RuleChain
import ru.aokazantsev.devatlas.MainActivity

typealias DevAtlasComposeRule = AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>

class DevAtlasE2e {
    val server = MockServerRule()
    val compose: DevAtlasComposeRule = createAndroidComposeRule<MainActivity>()
    val chain: RuleChain = RuleChain.outerRule(server).around(ExternalIntentsRule()).around(compose)
}

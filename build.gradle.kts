import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt)
}

val detektRules = projects.detektRules

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom(
            fileTree("src") {
                include("**/kotlin/**/*.kt", "**/java/**/*.kt")
            },
            fileTree(".") {
                include("*.kts")
            },
        )
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        exclude("**/build/**")
        reports {
            html.required.set(true)
            xml.required.set(false)
            sarif.required.set(false)
            txt.required.set(false)
            md.required.set(false)
        }
    }

    if (path != detektRules.path) {
        dependencies {
            "detektPlugins"(detektRules)
        }
    }
}

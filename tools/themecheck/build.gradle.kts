import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(projects.themeformat)
    testImplementation(libs.kotlin.test)
}

application {
    mainClass.set("ru.aokazantsev.devatlas.themecheck.MainKt")
    applicationName = "themecheck"
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

tasks.test {
    workingDir = rootDir
}

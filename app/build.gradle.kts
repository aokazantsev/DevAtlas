import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val releaseSigning: Properties? = run {
    val local = Properties()
    rootProject.file("local.properties").takeIf { it.isFile }?.reader(Charsets.UTF_8)?.use(local::load)
    val path = providers.environmentVariable("DEVATLAS_SIGNING").orNull ?: local.getProperty("devatlas.signing")
    path?.let(rootProject::file)?.let { signingFile ->
        if (!signingFile.isFile) throw GradleException("Файл подписи не найден: $signingFile")
        val signing = Properties().apply { signingFile.reader(Charsets.UTF_8).use(::load) }
        val missing = listOf("storeFile", "storePassword", "keyAlias", "keyPassword").filter { signing.getProperty(it).isNullOrBlank() }
        if (missing.isNotEmpty()) throw GradleException("В $signingFile нет ключей: ${missing.joinToString()}")
        val store = signingFile.parentFile.resolve(signing.getProperty("storeFile"))
        if (!store.isFile) throw GradleException("Хранилище ключа не найдено: $store")
        signing.setProperty("storeFile", store.path)
        signing
    }
}

android {
    namespace = "ru.aokazantsev.devatlas"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ru.aokazantsev.devatlas"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.1.2"
        testInstrumentationRunner = "ru.aokazantsev.devatlas.e2e.DevAtlasTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["timeout_msec"] = "180000"
    }

    signingConfigs {
        releaseSigning?.let { signing ->
            create("release") {
                storeFile = File(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    flavorDimensions += "ssl"
    productFlavors {
        create("ssl") {
            dimension = "ssl"
        }
        create("noSsl") {
            dimension = "ssl"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

abstract class SyncSampleThemeAssets : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sampleTheme: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun sync() {
        val target = output.get().asFile.resolve("sample-theme")
        target.deleteRecursively()
        sampleTheme.get().asFile.copyRecursively(target)
    }
}

val syncSampleThemeAssets = tasks.register<SyncSampleThemeAssets>("syncSampleThemeAssets") {
    sampleTheme.set(rootProject.layout.projectDirectory.dir("sample-theme"))
}

androidComponents {
    beforeVariants { variant ->
        if (variant.productFlavors.contains("ssl" to "noSsl") && variant.buildType == "release") {
            variant.enable = false
        }
    }
    onVariants { variant ->
        variant.androidTest?.sources?.assets?.addGeneratedSourceDirectory(syncSampleThemeAssets, SyncSampleThemeAssets::output)
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.espresso.web)
    androidTestImplementation(libs.mockwebserver)
    androidTestUtil(libs.androidx.test.orchestrator)
}

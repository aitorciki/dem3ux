import net.aitorciki.dem3ux.build.GenerateBridgePresetManifestTask
import net.aitorciki.dem3ux.build.GenerateBridgePresetsTask
import net.aitorciki.dem3ux.build.RefreshBridgePresetsFromEsDeTask
import net.aitorciki.dem3ux.build.ValidateBridgePresetAliasesTask
import org.gradle.api.GradleException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use(::load)
        }
    }

fun keystoreProperty(name: String): String? = keystoreProperties.getProperty(name)?.takeIf { value -> value.isNotBlank() }

val hasReleaseSigningProperties =
    listOf("storeFile", "storePassword", "keyAlias")
        .all { name -> keystoreProperty(name) != null }

val releaseKeyPassword: String?
    get() = keystoreProperty("keyPassword") ?: keystoreProperty("storePassword")

val bridgePresetCatalogFile = rootProject.file("presets/bridge-presets.json")
val generatedPresetSourceFile = layout.projectDirectory.file("src/main/java/net/aitorciki/dem3ux/bridge/PresetBridges.kt")
val generatedPresetManifestFile = layout.buildDirectory.file("generated/bridgePresetManifest/AndroidManifest.xml")
val esDeSystemsUrl = "https://gitlab.com/es-de/emulationstation-de/-/raw/master/resources/systems/android/es_systems.xml"
val esDeFindRulesUrl = "https://gitlab.com/es-de/emulationstation-de/-/raw/master/resources/systems/android/es_find_rules.xml"

val refreshBridgePresetsFromEsDe by tasks.registering(RefreshBridgePresetsFromEsDeTask::class) {
    group = "build"
    description = "Refreshes presets/bridge-presets.json from ES-DE Android emulator metadata."
    esSystemsUrl.set(esDeSystemsUrl)
    esFindRulesUrl.set(esDeFindRulesUrl)
    catalogFile.set(bridgePresetCatalogFile)
}

val generateBridgePresets by tasks.registering(GenerateBridgePresetsTask::class) {
    group = "build"
    description = "Generates bridge preset registry source from presets/bridge-presets.json."
    catalogFile.set(bridgePresetCatalogFile)
    outputFile.set(generatedPresetSourceFile)
}

val formatGeneratedBridgePresets by tasks.registering {
    group = "formatting"
    description = "Generates bridge preset registry source and formats it with Spotless."
    dependsOn(generateBridgePresets)
    finalizedBy(rootProject.tasks.named("spotlessApply"))
}

val generateBridgePresetManifest by tasks.registering(GenerateBridgePresetManifestTask::class) {
    group = "build"
    description = "Generates preset activity aliases manifest from presets/bridge-presets.json."
    catalogFile.set(bridgePresetCatalogFile)
    manifestFile.set(generatedPresetManifestFile)
}

val validateBridgePresetAliases by tasks.registering(ValidateBridgePresetAliasesTask::class) {
    group = "verification"
    description = "Validates AndroidManifest preset aliases against presets/bridge-presets.json."
    catalogFile.set(bridgePresetCatalogFile)
    generatedManifestFile.set(generateBridgePresetManifest.flatMap { task -> task.manifestFile })
    mainManifestFile.set(file("src/main/AndroidManifest.xml"))
    dependsOn(generateBridgePresetManifest)
}

val syncBridgePresets by tasks.registering {
    group = "build"
    description = "Refreshes bridge presets from ES-DE, regenerates registry source, and formats it."
    dependsOn(refreshBridgePresetsFromEsDe, formatGeneratedBridgePresets)
}

generateBridgePresets {
    mustRunAfter(refreshBridgePresetsFromEsDe)
}

formatGeneratedBridgePresets {
    mustRunAfter(refreshBridgePresetsFromEsDe)
}

generateBridgePresetManifest {
    mustRunAfter(refreshBridgePresetsFromEsDe)
}

rootProject.tasks.named("spotlessApply") {
    mustRunAfter(formatGeneratedBridgePresets)
}

gradle.taskGraph.whenReady {
    val releaseArtifactRequested =
        allTasks.any { task ->
            task.project == project &&
                task.name in setOf("assemble", "assembleRelease", "bundleRelease", "packageRelease")
        }

    if (releaseArtifactRequested && !hasReleaseSigningProperties) {
        throw GradleException(
            "Release signing requires a complete root keystore.properties file. " +
                "Use keystore.properties.example as a template.",
        )
    }
}

android {
    namespace = "net.aitorciki.dem3ux"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.aitorciki.dem3ux"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "OldTargetApi")
    }

    signingConfigs {
        if (hasReleaseSigningProperties) {
            create("release") {
                storeFile = file(requireNotNull(keystoreProperty("storeFile")))
                storePassword = requireNotNull(keystoreProperty("storePassword"))
                keyAlias = requireNotNull(keystoreProperty("keyAlias"))
                keyPassword = requireNotNull(releaseKeyPassword)
                storeType = keystoreProperty("storeType") ?: "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigningProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants { variant ->
        variant.sources.manifests.addGeneratedManifestFile(
            generateBridgePresetManifest,
            GenerateBridgePresetManifestTask::manifestFile,
        )
    }
}

tasks.named("preBuild") {
    dependsOn(generateBridgePresetManifest)
}

tasks.named("lint") {
    dependsOn(validateBridgePresetAliases)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}

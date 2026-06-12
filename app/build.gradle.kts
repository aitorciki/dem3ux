import org.gradle.api.GradleException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
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
        versionCode = 1
        versionName = "0.1.0"

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

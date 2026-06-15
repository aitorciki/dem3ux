plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.diffplug.spotless") version "8.6.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.github.pdvrieze.xmlutil:serialization:1.0.0-rc3")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.8.0")
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.8.0")
    }
}

tasks.register("verify") {
    group = "verification"
    description = "Runs formatting checks."
    dependsOn("spotlessCheck")
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target(
            "app/src/**/*.kt",
        )
        targetExclude("**/build/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }

    kotlinGradle {
        target("**/*.gradle.kts", "**/*.main.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

tasks.named("spotlessKotlin") {
    mustRunAfter(":app:generateBridgePresets")
}

tasks.register("verify") {
    group = "verification"
    description = "Runs formatting checks, Android lint, and unit tests."
    dependsOn("spotlessCheck", ":app:lint", ":app:testDebugUnitTest")
}

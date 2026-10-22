// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "resolum_alivia-android-application")
        property("sonar.organization", "resolum")
        property(
            "sonar.androidLint.reportPaths",
            "${project.projectDir}/app/build/reports/lint-results-debug.xml"
        )
        property(
            "sonar.exclusions",
            "**/*.webp,**/*.png,**/*.jpg,**/*.jpeg,**/*.gif,**/*.ico"
        )
    }
}

tasks.named("sonar") {
    dependsOn(
        ":app:lintDebug",
        ":app:testDebugUnitTest",
        ":app:assembleDebug"
    )
}
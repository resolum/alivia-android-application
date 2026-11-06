// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.app.distribution) apply false
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
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project.projectDir}/app/build/reports/coverage/test/debug/report.xml"
        )
        // Compose screens, theme, navigation, DI wiring and the Android glue (DataStore, Retrofit/OkHttp
        // construction) are not reachable from JVM unit tests; the rules they use are tested where they live.
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/presentation/**",
                "**/core/ui/**",
                "**/core/navigation/**",
                "**/di/**",
                "**/MainActivity.kt",
                "**/AliviaApplication.kt",
                "**/datastore/TokenDataStore.kt",
                "**/remote/ApiClientFactory.kt",
            ).joinToString(",")
        )
    }
}

tasks.named("sonar") {
    dependsOn(
        ":app:lintDebug",
        ":app:createDebugUnitTestCoverageReport",
        ":app:assembleDebug"
    )
}

import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.app.distribution)
}

val releaseKeystorePath = providers.environmentVariable("ALIVIA_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("ALIVIA_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ALIVIA_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ALIVIA_KEY_PASSWORD").orNull
val apiBaseUrlOverride = providers.gradleProperty("apiBaseUrl").orNull

android {
    namespace = "com.resolum.alivia"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.resolum.alivia"
        minSdk = 28
        targetSdk = 36
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("appVersionName").orNull ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    val ciReleaseSigning = if (
        releaseKeystorePath != null &&
        releaseKeystorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null
    ) {
        signingConfigs.create("ciRelease") {
            storeFile = file(releaseKeystorePath)
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            ciReleaseSigning?.let { signingConfig = it }
            val releaseApiBaseUrl = apiBaseUrlOverride ?: "https://invalid.invalid/"
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            // R8 shrinks and obfuscates the release; proguard-rules.pro keeps what Gson reads by reflection.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            firebaseAppDistribution {
                artifactType = "APK"
                testers = "faridce14@gmail.com,sdiaz4519@gmail.com"
                releaseNotes = providers.environmentVariable("FIREBASE_RELEASE_NOTES").orNull
                    ?: "Alivia Android ${providers.gradleProperty("appVersionName").orNull ?: "local"}"
            }
        }
        debug {
            // The Android emulator reaches the development machine through 10.0.2.2.
            val debugApiBaseUrl = apiBaseUrlOverride ?: "http://10.0.2.2:8080/"
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            // JaCoCo report of the JVM unit tests (createDebugUnitTestCoverageReport), read by SonarCloud.
            enableUnitTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

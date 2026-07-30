plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "app.traced_it"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.traced_it"
        minSdk = 25
        // noinspection EditedTargetSdkVersion
        targetSdk = 37
        versionCode = 16
        versionName = "1.7.1"

        vectorDrawables {
            useSupportLibrary = true
        }

        androidResources {
            @Suppress("UnstableApiUsage")
            localeFilters += listOf(
                "ar",
                "cs",
                "de",
                "en",
                "fr",
                "iw",
                "nl",
                "pl",
                "pt-rBR",
                "ru",
            )
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The following argument makes the Android Test Orchestrator run its "pm clear" command after each test
        // invocation. This command ensures that the app's state is completely cleared between tests.
        testInstrumentationRunnerArguments += mapOf("clearPackageData" to "true")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }
    flavorDimensions += "tier"
    productFlavors {
        create("free") {
            isDefault = true
            dimension = "tier"
        }
        create("pro") {
            dimension = "tier"
            applicationIdSuffix = ".pro"
        }
        create("demo") {
            dimension = "tier"
            applicationIdSuffix = ".demo"
        }
    }
    buildFeatures {
        aidl = false
        buildConfig = true
        compose = true
        shaders = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
    lint {
        disable += "MissingTranslation"
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

room {
    // Enable room auto-migrations.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.testing)
    implementation(libs.androidx.room.paging)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.testing)
    implementation(libs.commons.csv)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.collections.immutable)
    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestUtil(libs.androidx.test.orchestrator)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.keyframeplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.keyframeplayer"
        minSdk = 26
        targetSdk = 34

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_1_8

        targetCompatibility =
            JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion =
            "1.5.1"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.activity.compose)

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(libs.androidx.ui)

    implementation(
        libs.androidx.ui.graphics
    )

    implementation(
        libs.androidx.ui.tooling.preview
    )

    implementation(libs.androidx.material3)

    // Grid用
    implementation(
        "androidx.compose.foundation:foundation"
    )

    // Navigation
    implementation(
        "androidx.navigation:navigation-compose:2.7.7"
    )

    // 動画再生
    implementation(
        "androidx.media3:media3-exoplayer:1.3.1"
    )

    implementation(
        "androidx.media3:media3-ui:1.3.1"
    )

    // ViewModel
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7"
    )

    testImplementation(libs.junit)

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    debugImplementation(
        libs.androidx.ui.tooling
    )
}
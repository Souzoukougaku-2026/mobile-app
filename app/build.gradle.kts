plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.example.keyframeplayer"
    compileSdk = 36 // SDK 36に引き上げ

    defaultConfig {
        applicationId = "com.example.keyframeplayer"
        minSdk = 30 // 移植元に合わせて30に設定（安全のため）
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // --- 共通・Compose基本 (libs経由) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)

    // --- 移植先（現在）の固有ライブラリ ---
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer) // 動画再生
    implementation(libs.androidx.media3.ui)        // 動画UI

    implementation(libs.litert)
    implementation(libs.google.litert.gpu)
    implementation(libs.litert.gpu.api)
    implementation(libs.litert.support.api)
    implementation(libs.litert.metadata)

    implementation(libs.androidx.compose.material.icons.extended) // 拡張アイコン
    implementation(libs.androidx.documentfile)          // ファイル操作

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ViewModel & Lifecycle (最新の 2.11.0 に統一)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // --- テスト関連 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
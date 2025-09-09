// build.gradle.kts (app level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // Make sure this line is present
}

android {
    namespace = "com.example.mine"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mine"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }
}

dependencies {
    // Core Android
    implementation("org.lz4:lz4-java:1.8.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material.icons.extended)
    
    // Compose Navigation
    implementation(libs.androidx.navigation.compose)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.generativeai)
    kapt(libs.androidx.room.compiler)
    implementation("androidx.navigation:navigation-compose:2.7.3")
    // Cryptography
    implementation(libs.androidx.security.crypto)
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)
    
    // Compression - Using Java's built-in Deflate/Inflate
    
    // Bluetooth LE - Using native Android Bluetooth APIs

    // Compose BOM (keeps versions aligned)
    implementation ("androidx.compose:compose-bom:2024.08.00")

// Core Compose UI
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    debugImplementation ("androidx.compose.ui:ui-tooling")

// Material 3
    implementation ("androidx.compose.material3:material3")

// Icons (needed for ArrowBack, Shield, Send, etc.)
    implementation ("androidx.compose.material:material-icons-core")
    implementation ("androidx.compose.material:material-icons-extended")

// Foundation & animation
    implementation ("androidx.compose.foundation:foundation")
    implementation ("androidx.compose.animation:animation")

    // Network & WiFi
    implementation(libs.androidx.work.runtime.ktx)
    
    // QR Code Generation
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    
    // Camera for QR scanning
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    
    // ML Kit for QR code scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // JSON parsing for QR code data
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.compose.ui:ui:1.6.2")
    implementation("androidx.compose.ui:ui-graphics:1.6.2")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
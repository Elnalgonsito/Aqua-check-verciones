@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Mantenemos Compose, aunque la lógica principal use XML
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.aqua_check"
    // Usamos el formato estándar de Android Studio para el API Level
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.aqua_check"
        minSdk = 26 // Reducimos el mínimo para mayor compatibilidad de BT
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

    // ===============================================
    // CAMBIOS CLAVE: Habilitar ViewBinding para el Layout XML
    // ===============================================
    buildFeatures {
        compose = true
        viewBinding = true // <--- AÑADIDO: NECESARIO para ActivityMainBinding
    }
}

dependencies {
    // === DEPENDENCIAS DE FRAMEWORK Y LÓGICA ===
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) // Necesario para lifecycleScope

    // Coroutines (Obligatorio para la lógica asíncrona de BluetoothSPPService)
    implementation(libs.kotlinx.coroutines.android)

    // CardView (Obligatorio para el layout activity_main.xml)
    implementation(libs.cardview)

    // === DEPENDENCIAS DE COMPOSE (Mantenidas) ===
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat) // AppCompat para las Views Clásicas
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // === TESTS ===
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
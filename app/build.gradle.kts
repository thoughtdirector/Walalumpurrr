plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.notificacionesapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.notificacionesapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Navegación
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Componentes de Android existentes
    implementation(libs.androidx.ui.graphics.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.material3.android)

    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")

    // Firebase Cloud Messaging
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Sign-In (añadido para autenticación con Google)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
// OkHttp para las llamadas FCM
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

// WorkManager para tareas periódicas
    implementation("androidx.work:work-runtime-ktx:2.9.0")

}

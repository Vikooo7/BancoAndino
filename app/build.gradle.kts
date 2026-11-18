plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.bancoandino"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.bancoandino"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        // URL de la API: la de Vercel, o otra al compilar con -PurlApi=http://localhost:3100/ (pruebas en la PC).
        buildConfigField("String", "URL_API", "\"" + (providers.gradleProperty("urlApi").orNull ?: "https://app-api-rutalog.vercel.app/") + "\"")
        versionName = "2.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // API del núcleo de Banco Andino (/banco/api): Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    // Sesión cifrada en el teléfono
    implementation(libs.androidx.security.crypto)
    // Lector de QR con la cámara, sin pedir permiso de cámara
    implementation(libs.play.services.code.scanner)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

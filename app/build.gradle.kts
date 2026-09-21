plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.todoaccesible"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.todoaccesible"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Se deja sin la IP de esta PC a propósito: quien levante el backend en su propia
        // máquina debe poner aquí su IP de LAN (ver network_security_config.xml, que también
        // hay que actualizar para permitir tráfico HTTP hacia esa IP).
        buildConfigField("String", "API_BASE_URL", "\"\"")
        buildConfigField("String", "SOCKET_BASE_URL", "\"\"")
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
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Iconos de Material
    implementation(libs.androidx.compose.material.icons.extended)

    // Retrofit / red
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.kotlinx.coroutines.android)
    // NOTA: originalmente `debugImplementation`, cambiado a `implementation` porque
    // NetworkModule.kt (código compartido debug/release) referencia HttpLoggingInterceptor
    // directamente -aunque solo la ACTIVA en tiempo de ejecución si BuildConfig.DEBUG-, y
    // `debugImplementation` no está en el classpath de compilación de la variante release
    // (rompía `compileReleaseKotlin` / `./gradlew test`, que corre ambas variantes).
    implementation(libs.okhttp.logging.interceptor)

    // Socket.IO (notificaciones/estado en vivo)
    implementation(libs.socket.io.client) {
        exclude(group = "org.json", module = "json")
    }

    // DataStore
    implementation(libs.datastore.preferences)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Image Loading
    implementation(libs.coil.compose)

    // Excel export
    implementation(libs.apache.poi.ooxml)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
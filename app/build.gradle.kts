plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.autoreply.bot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autoreply.bot"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.9"

        // Repositorio de GitHub usado por el buscador de actualizaciones.
        buildConfigField("String", "GITHUB_OWNER", "\"dloren-ops\"")
        buildConfigField("String", "GITHUB_REPO", "\"tu-usuario-AutoReply\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // Llave de firma FIJA compartida por todas las versiones. Esto evita el
        // error "conflicto con un paquete existente" al actualizar, porque la
        // firma del APK no cambia entre versiones.
        create("shared") {
            storeFile = file("autoreply-release.jks")
            storePassword = "autoreply123"
            keyAlias = "autoreply"
            keyPassword = "autoreply123"
        }
    }

    buildTypes {
        debug {
            // El CI compila el APK de debug; lo firmamos con la llave fija.
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("shared")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
}

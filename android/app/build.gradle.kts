import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val targetAppId = "com.kangekie.konvoitouring"
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    val content = runCatching { googleServicesFile.readText() }.getOrDefault("")
    val packageMatches = Regex(""""package_name"\s*:\s*"([^"]+)"""")
        .findAll(content)
        .map { it.groupValues[1].trim() }
        .toList()
    if (packageMatches.any { it == targetAppId }) {
        apply(plugin = "com.google.gms.google-services")
    } else {
        val foundDesc = if (packageMatches.isEmpty()) "none" else packageMatches.joinToString(", ")
        logger.warn(
            "[KonvoiKu] Skipping com.google.gms.google-services because google-services.json " +
                "has no client matching '$targetAppId' (found package_name: $foundDesc). " +
                "Using programmatic FirebaseOptions fallback."
        )
    }
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val tomtomApiKey: String = listOf("TOMTOM_API_KEY", "MAPS_API_KEY", "MPS_API_KEY")
    .firstNotNullOfOrNull { key ->
        localProps.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
    }
    ?: "fxxBdOwiITup2btha0dFUl6IOxczJm4f"

android {
    namespace = "com.konvoiku.touring"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kangekie.konvoitouring"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TOMTOM_API_KEY", "\"$tomtomApiKey\"")
    }

    val fixedDebugKeystore = file("debug.keystore")
    signingConfigs {
        getByName("debug") {
            if (fixedDebugKeystore.exists()) {
                storeFile = fixedDebugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Jetpack Compose BOM & Material Design 3
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // TomTom Tile Map Engine (osmdroid) & Fused Location GPS
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)

    // Firebase Firestore & Auth (Sinkronisasi Room Konvoi)
    val firebaseBom = platform(libs.firebase.bom)
    implementation(firebaseBom)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)

    // Testing & Debug Tooling
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

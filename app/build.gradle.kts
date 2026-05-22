plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.leonxlnx.imagesorter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.leonxlnx.imagesorter"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.1"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Stable keystore checked into the repo so every build of this open-source
        // project signs the same way, regardless of which machine builds it. This is
        // NOT a Play-Store release key; if you want to ship to Play, generate your
        // own key and override these values via ~/.gradle/gradle.properties or env.
        create("distribution") {
            storeFile = file("photoswipe-dev.jks")
            storePassword = "photoswipe"
            keyAlias = "photoswipe"
            keyPassword = "photoswipe"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
        // Force v1+v2+v3 on the default debug signing as well so sideloading the
        // debug APK works on Samsung / older devices that require JAR (v1) signing.
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign release builds with the distribution keystore so installation
            // does not collide with the per-machine debug keystore.
            signingConfig = signingConfigs.getByName("distribution")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")

    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

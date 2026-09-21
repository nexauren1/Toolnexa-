plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.toolnexa.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.toolnexa.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 42
        versionName = "1.30.2"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.19.0"
        )
    )
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation(
        "androidx.activity:activity-ktx:1.13.0"
    )
    implementation(
        "androidx.fragment:fragment-ktx:1.8.9"
    )
    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.11.0"
    )
    implementation(
        "androidx.credentials:credentials:1.3.0"
    )
    implementation(
        "androidx.credentials:credentials-play-services-auth:1.3.0"
    )
    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.1.1"
    )
    implementation(
        "androidx.core:core-ktx:1.19.0"
    )
    implementation(
        "com.google.android.gms:play-services-base:18.11.0"
    )
    implementation(
        "com.google.android.gms:play-services-tflite-java:16.5.0"
    )
}

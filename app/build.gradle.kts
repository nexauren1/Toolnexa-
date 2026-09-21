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
        versionCode = 36
        versionName = "1.27.0"
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
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.core:core-ktx:1.19.0")\n    implementation("org.tensorflow:tensorflow-lite:2.17.0")\n\n    val basicPitchModel = file("src/main/assets/basic_pitch_nmp.tflite")\n\n    tasks.register("ensureBasicPitchModel") {\n        outputs.file(basicPitchModel)\n        doLast {\n            if (!basicPitchModel.exists() || basicPitchModel.length() < 100000L) {\n                basicPitchModel.parentFile.mkdirs()\n                java.net.URI(\n                    "https://raw.githubusercontent.com/spotify/basic-pitch/main/basic_pitch/saved_models/icassp_2022/nmp.tflite"\n                ).toURL().openStream().use { input ->\n                    basicPitchModel.outputStream().use { output ->\n                        input.copyTo(output)\n                    }\n                }\n            }\n        }\n    }\n\n    tasks.named("preBuild").configure {\n        dependsOn("ensureBasicPitchModel")\n    }
}

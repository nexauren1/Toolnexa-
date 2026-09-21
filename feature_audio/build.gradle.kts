plugins {
    id("com.android.dynamic-feature")
}

android {
    namespace = "com.toolnexa.audio"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":app"))
    implementation(
        "com.google.android.gms:play-services-tflite-java:16.5.0"
    )
}

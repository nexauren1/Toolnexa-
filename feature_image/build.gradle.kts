plugins {
    id("com.android.dynamic-feature")
}

android {
    namespace = "com.toolnexa.image"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":app"))
    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.19.0"
        )
    )
    implementation(
        "com.google.firebase:firebase-auth"
    )
}

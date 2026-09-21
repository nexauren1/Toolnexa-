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
}

plugins {
    id("com.android.dynamic-feature")
}

android {
    namespace = "com.toolnexa.business"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":app"))
}

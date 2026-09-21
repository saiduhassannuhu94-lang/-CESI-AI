plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.cesi.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cesi.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.5.0"

        buildConfigField("String", "CESI_GEMINI_API_KEY", "\"${System.getenv("CESI_GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "CESI_AI_MODEL", "\"gemini-2.5-flash\"" )
    }
}

kotlin {
    jvmToolchain(17)
}

android.buildFeatures.compose = true

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

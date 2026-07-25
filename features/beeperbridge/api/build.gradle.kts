plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.api"
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}

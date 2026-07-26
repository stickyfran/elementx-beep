plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.api"
}

dependencies {
    implementation(projects.libraries.architecture)
    implementation(libs.androidx.annotationjvm)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}

plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.api"
}

dependencies {
    // Basic compose / android dependencies might be needed if BeeperNetwork uses DrawableRes
    implementation(libs.androidx.annotation)
}

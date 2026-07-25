plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.test"
}

dependencies {
    implementation(projects.features.beeperbridge.api)
}

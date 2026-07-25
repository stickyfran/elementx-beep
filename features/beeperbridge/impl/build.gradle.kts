plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.impl"
}

dependencies {
    implementation(projects.features.beeperbridge.api)
    // Needs access to Matrix client/types in real code, omitting for now to just compile isolated logic
}

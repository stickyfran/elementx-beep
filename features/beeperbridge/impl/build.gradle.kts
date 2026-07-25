import extension.setupDependencyInjection

plugins {
    id("io.element.android-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.impl"
}

setupDependencyInjection()

dependencies {
    implementation(projects.features.beeperbridge.api)
    implementation(projects.libraries.di)
}

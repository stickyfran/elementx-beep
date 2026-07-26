import extension.setupDependencyInjection

plugins {
    id("io.element.android-compose-library")
}

android {
    namespace = "io.element.android.features.beeperbridge.impl"
}

setupDependencyInjection()

dependencies {
    implementation(projects.features.beeperbridge.api)
    implementation(projects.libraries.di)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.json)
    implementation(libs.kotlinx.coroutines.core)

    testCommonDependencies(libs, true)
    testImplementation(projects.libraries.matrix.test)
}

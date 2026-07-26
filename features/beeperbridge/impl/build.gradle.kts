import extension.setupDependencyInjection
import extension.testCommonDependencies

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
    implementation(projects.libraries.designsystem)
    implementation(projects.libraries.compound)
    implementation(projects.libraries.architecture)
    implementation(projects.libraries.preferences.api)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)

    testCommonDependencies(libs, true)
    testImplementation(projects.libraries.matrix.test)
}

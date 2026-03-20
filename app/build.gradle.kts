plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "sevak.meliqsetyan.samsung_project_2"

    // ИСПРАВЛЕНО: Установите версию 36
    compileSdk = 36

    defaultConfig {
        applicationId = "sevak.meliqsetyan.samsung_project_2"
        minSdk = 26

        // РЕКОМЕНДУЕТСЯ: Также обновите до 36
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Календарь ProlificInteractive УДАЛЕН. Используем встроенный.

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
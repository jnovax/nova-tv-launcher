plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "vn.familytv.parentalcontrol"
    compileSdk = 34
    defaultConfig {
        applicationId = "vn.familytv.parentalcontrol"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-m1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:contract"))
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.lockbox"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.lockbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room components
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // SQLCipher for encrypted database storage
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite:2.4.0")
}
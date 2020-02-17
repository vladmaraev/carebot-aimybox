plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(25)
        targetSdkVersion(29)

        versionName = "1.0.0"
        versionCode = 1

        ndk {
            abiFilters("armeabi-v7a", "x86", "armeabi", "mips")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            //TODO configure pro guard
        }
    }

    lintOptions {
        isCheckAllWarnings = true
        isWarningsAsErrors = false
        isAbortOnError = true
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(files("libs/NuanceMobileToolkit.jar"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.1")
    implementation("com.justai.aimybox:core:0.9.0")
}
plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {

    compileSdkVersion(29)

    defaultConfig {
        applicationId = "com.justai.aimybox.smartscreen"

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

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/INDEX.LIST")
    }
}

dependencies {
    implementation(project(":nuancetrigger"))

    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference:1.1.0")

    implementation("com.google.android.material:material:1.0.0")

    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.core:core-ktx:1.1.0")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.1")

    implementation("com.justai.aimybox:core:0.11.0")
    implementation("com.justai.aimybox:components:0.1.8")

    implementation("com.github.bumptech.glide:glide:4.9.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.9.0")

    implementation("com.google.android.exoplayer:exoplayer:2.11.2")
    implementation("com.yandex.android:speechkit:3.12.2")
}
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val versionMajor = project.property("APP_VERSION_MAJOR").toString().toInt()
val versionMinor = project.property("APP_VERSION_MINOR").toString().toInt()

android {
    namespace = "com.github.fractals"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.github.fractals"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = (versionMajor * 100) + versionMinor
        versionName = "${versionMajor}.${versionMinor}"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = project.property("STORE_PASSWORD_RELEASE") as String
            keyAlias = "release"
            keyPassword = project.property("KEY_PASSWORD_RELEASE") as String
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFile(getDefaultProguardFile("proguard-android-optimize.txt"))
            proguardFile("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    lint {
        disable += "AllowBackup"
        disable += "AlwaysShowAction"
        disable += "ClickableViewAccessibility"
        disable += "GoogleAppIndexingWarning"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
}

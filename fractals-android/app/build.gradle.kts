plugins {
    id("com.android.application")
    kotlin("android")
}

val versionMajor = project.property("APP_VERSION_MAJOR").toString().toInt()
val versionMinor = project.property("APP_VERSION_MINOR").toString().toInt()

android {
    compileSdk = Versions.androidBuildSdk
    namespace = "com.github.fractals"

    defaultConfig {
        applicationId = "com.github.fractals"
        minSdk = Versions.androidBuildMinSdk
        targetSdk = Versions.androidBuildTargetSdk
        versionCode = generateVersionCode(versionMajor, versionMinor)
        versionName = "${versionMajor}.${versionMinor.toString().padStart(2, '0')}"
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
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFile(getDefaultProguardFile("proguard-android.txt"))
            proguardFile("proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = Versions.jvm
        targetCompatibility = Versions.jvm
    }

    kotlinOptions {
        jvmTarget = Versions.jvm.toString()
    }

    lint {
        disable += "AllowBackup"
        disable += "AlwaysShowAction"
        disable += "ClickableViewAccessibility"
        disable += "GoogleAppIndexingWarning"
    }
}

dependencies {
    // Jetpack
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // TODO migrate Rx to Flow
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // Testing
    testImplementation("junit:junit:4.13.2")
}

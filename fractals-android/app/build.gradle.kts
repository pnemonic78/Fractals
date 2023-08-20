plugins {
    id("com.android.application")
    kotlin("android")
}

val versionMajor = (project.property("APP_VERSION_MAJOR") as String).toInt()
val versionMinor = (project.property("APP_VERSION_MINOR") as String).toInt()

android {
    compileSdk = Versions.androidBuildSdkVersion

    defaultConfig {
        applicationId = "com.github.fractals"
        minSdk = Versions.androidBuildMinSdkVersion
        targetSdk = Versions.androidBuildTargetSdkVersion
        versionCode = generateVersionCode(versionMajor, versionMinor)
        versionName = "${versionMajor}.${versionMinor.toString().padLeft(2, "0")}"
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
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir("src/main/kotlin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    lintOptions {
        disable("AllowBackup")
        disable("AlwaysShowAction")
        disable("ClickableViewAccessibility")
        disable("GoogleAppIndexingWarning")
    }
}

dependencies {
    // Rx
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // AndroidX
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
}

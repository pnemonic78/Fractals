import org.gradle.api.JavaVersion

object BuildVersions {
    const val agp = "8.7.3"
    const val kotlin = "2.1.0"
    val jvm = JavaVersion.VERSION_1_8

    const val minSdk = 21
    const val compileSdk = 35
    const val targetSdk = 35
}
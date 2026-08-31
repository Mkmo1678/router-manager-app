plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.toInt()
    } catch (e: Exception) {
        1
    }
}

val commitCount = getGitCommitCount()

android {
    namespace = "com.router.manager"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.router.manager"
        minSdk = 26
        targetSdk = 34
        versionCode = commitCount
        versionName = "1.0.$commitCount"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

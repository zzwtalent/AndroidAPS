import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.ksp)
    id("com.android.application")
    id("kotlin-android")
    id("android-app-dependencies")
    id("test-app-dependencies")
    id("jacoco-app-dependencies")
}

repositories {
    google()
    mavenCentral()
}

fun generateGitBuild(): String {
    try {
        val processBuilder = ProcessBuilder("git", "describe", "--always")
        val output = File.createTempFile("git-build", "")
        processBuilder.redirectOutput(output)
        val process = processBuilder.start()
        process.waitFor()
        return output.readText().trim()
    } catch (_: Exception) {
        return "NoGitSystemAvailable"
    }
}

fun generateDate(): String {
    val stringBuilder: StringBuilder = StringBuilder()
    // showing only date prevents app to rebuild everytime
    stringBuilder.append(SimpleDateFormat("yyyy.MM.dd").format(Date()))
    return stringBuilder.toString()
}

val keyProps = Properties()
val keyPropsFile: File = rootProject.file("keystore/keystore.properties")
if (keyPropsFile.exists()) {
    keyProps.load(FileInputStream(keyPropsFile))
}

fun getStoreFile(): String {
    var storeFile = keyProps["storeFile"].toString()
    if (storeFile.isEmpty()) {
        storeFile = System.getenv("storeFile") ?: ""
    }
    return storeFile
}

fun getStorePassword(): String {
    var storePassword = keyProps["storePassword"].toString()
    if (storePassword.isEmpty()) {
        storePassword = System.getenv("storePassword") ?: ""
    }
    return storePassword
}

fun getKeyAlias(): String {
    var keyAlias = keyProps["keyAlias"].toString()
    if (keyAlias.isEmpty()) {
        keyAlias = System.getenv("keyAlias") ?: ""
    }
    return keyAlias
}

fun getKeyPassword(): String {
    var keyPassword = keyProps["keyPassword"].toString()
    if (keyPassword.isEmpty()) {
        keyPassword = System.getenv("keyPassword") ?: ""
    }
    return keyPassword
}

android {
    namespace = "app.aaps.wear"

    defaultConfig {
        minSdk = Versions.wearMinSdk
        targetSdk = Versions.wearTargetSdk

        buildConfigField("String", "BUILDVERSION", "\"${generateGitBuild()}-${generateDate()}\"")
    }

    signingConfigs {
        create("release") {
            if (getStoreFile().isNotEmpty() && getStorePassword().isNotEmpty() && getKeyAlias().isNotEmpty() && getKeyPassword().isNotEmpty()) {
                storeFile = file(getStoreFile())
                storePassword = getStorePassword()
                keyAlias = getKeyAlias()
                keyPassword = getKeyPassword()
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            // Disable androidTest coverage, since it performs offline coverage
            // instrumentation and that causes online (JavaAgent) instrumentation
            // to fail in this project.
            enableAndroidTestCoverage = false
        }
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    flavorDimensions.add("standard")
    productFlavors {
        create("full") {
            isDefault = true
            applicationId = "info.nightscout.androidaps"
            dimension = "standard"
            versionName = Versions.appVersion
        }
        create("pumpcontrol") {
            applicationId = "info.nightscout.aapspumpcontrol"
            dimension = "standard"
            versionName = Versions.appVersion + "-pumpcontrol"
        }
        create("aapsclient") {
            applicationId = "info.nightscout.aapsclient"
            dimension = "standard"
            versionName = Versions.appVersion + "-aapsclient"
        }
        create("aapsclient2") {
            applicationId = "info.nightscout.aapsclient2"
            dimension = "standard"
            versionName = Versions.appVersion + "-aapsclient2"
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

allprojects {
    repositories {
    }
}

dependencies {
    implementation(project(":shared:impl"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:keys"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.legacy.support)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.wear)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.constraintlayout)

    testImplementation(project(":shared:tests"))

    compileOnly(libs.com.google.android.wearable)
    implementation(libs.com.google.android.wearable.support)
    implementation(libs.com.google.android.gms.playservices.wearable)
    implementation(files("${rootDir}/wear/libs/ustwo-clockwise-debug.aar"))
    implementation(files("${rootDir}/wear/libs/wearpreferenceactivity-0.5.0.aar"))
    implementation(files("${rootDir}/wear/libs/hellocharts-library-1.5.8.aar"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.stdlib.jdk8)

    ksp(libs.com.google.dagger.android.processor)
    ksp(libs.com.google.dagger.compiler)
}

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.register

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.utopia.finance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.utopia.finance"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

val debugUnitTestWorkDir = File(System.getProperty("java.io.tmpdir"), "personal-finance-testDebugUnitTest")
val syncDebugMainClassesForTest by tasks.registering(Sync::class) {
    dependsOn("compileDebugKotlin")
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
    into(File(debugUnitTestWorkDir, "main"))
}

val syncDebugUnitTestClassesForTest by tasks.registering(Sync::class) {
    dependsOn("compileDebugUnitTestKotlin")
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"))
    into(File(debugUnitTestWorkDir, "test"))
}

afterEvaluate {
    tasks.named<Test>("testDebugUnitTest").configure {
        dependsOn(syncDebugMainClassesForTest, syncDebugUnitTestClassesForTest)
        val mainDir = File(debugUnitTestWorkDir, "main")
        val testDir = File(debugUnitTestWorkDir, "test")
        testClassesDirs = files(testDir)
        classpath = files(
            testDir,
            mainDir,
            configurations.getByName("debugUnitTestRuntimeClasspath"),
        )
    }
}

val domainTestWorkDir = File(System.getProperty("java.io.tmpdir"), "personal-finance-domain-test")
val syncDomainMainClasses by tasks.registering(Sync::class) {
    dependsOn("compileDebugKotlin")
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
    into(File(domainTestWorkDir, "main"))
}

val syncDomainTestClasses by tasks.registering(Sync::class) {
    dependsOn("compileDebugUnitTestKotlin")
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"))
    into(File(domainTestWorkDir, "test"))
}

val domainTest by tasks.registering(Test::class) {
    description = "Runs fast JVM tests for pure finance domain logic."
    group = "verification"
    val debugClasses = File(domainTestWorkDir, "main")
    val testClasses = File(domainTestWorkDir, "test")
    dependsOn(syncDomainMainClasses, syncDomainTestClasses)
    testClassesDirs = files(testClasses)
    classpath = files(
        testClasses,
        debugClasses,
        configurations.getByName("debugUnitTestRuntimeClasspath"),
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

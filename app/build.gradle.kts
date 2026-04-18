import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.room)
    alias(libs.plugins.hiltAndroid)
    id("com.google.protobuf")
}

android {
    namespace = "com.mato.syai"
    compileSdk = 36


    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(localPropertiesFile.inputStream())
    }

    defaultConfig {
        applicationId = "com.mato.syai"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey = localProperties.getProperty("GEMINI_API") ?: ""
        buildConfigField("String", "GEMINI_API", "\"$apiKey\"")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

protobuf {
    protoc {
        // Match this version to your dependencies
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // Generate Java Lite code
                create("java") {
                    option("lite")
                }
                // Generate Kotlin Lite code
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.firebase.config.ktx)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.compose.android)

    // LiveData
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.runtime.livedata)

    // Generative AI
    implementation(libs.generativeai)

    // Rive
    implementation(libs.rive.android)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Gson
    implementation(libs.gson)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coil (Image loading)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // App Startup
    implementation(libs.androidx.startup.runtime)

    // Networking
    implementation(libs.okhttp)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)


    implementation(libs.kotlinx.coroutines.jdk8)

    implementation (libs.firebase.auth)
    implementation (libs.androidx.credentials)
    implementation (libs.play.services.auth)
    implementation (libs.glide)
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.android.gms:play-services-auth:21.1.1")
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.3.0")
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
    implementation("com.google.protobuf:protobuf-kotlin-lite:3.25.1")
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.airbnb.android:lottie:6.7.1")
    implementation("com.airbnb.android:lottie-compose:6.7.1")

}
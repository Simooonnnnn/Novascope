// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Add the Compose Compiler plugin explicitly for Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.novascope"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.novascope"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Add this to download the model automatically during build
    // (would be implemented in a real production app)
    /*
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }

    tasks.register<DownloadModelTask>("downloadSmolLMModel") {
        outputDir = file("src/main/assets")
        modelUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-135M/resolve/main/model_optimized.tflite"
        modelName = "smollm2_article_summarizer.tflite"
    }

    tasks.named("preBuild") {
        dependsOn("downloadSmolLMModel")
    }
    */
}

dependencies {
    // JSON parsing with Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Accompanist System UI Controller and SwipeRefresh
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.15.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
    // For text processing models
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // RSS Parsing Library
    implementation("com.rometools:rome:1.19.0")  // Updated version

    // Core Android libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM and UI
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3 with latest version that includes PullToRefreshBox
    implementation("androidx.compose.material3:material3:1.3.2")

    // Animation
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")

    // Material 3 and Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // RSS parser library
    implementation("com.prof18.rssparser:rssparser:6.0.3")
    implementation("io.coil-kt:coil-compose:2.5.0") // Or the latest version

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.compose.ui:ui-graphics:1.6.8") // Use the latest version

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
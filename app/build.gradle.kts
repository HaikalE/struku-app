plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.struku"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.struku"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Force app to use OpenGL ES 3.0 or higher for GPU workloads
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
        prefab = true  // Properly configured prefab feature
        // Disable data binding to avoid the error with dataBindingMergeDependencyArtifactsDebug
        dataBinding = false
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/DEPENDENCIES"
            // Exclude native libraries that might conflict
            pickFirsts += "**/*.so"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Core Android libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Desugaring for compatibility with newer Java features on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    
    // ConstraintLayout - Added for XML layouts
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Compose - Use stable BOM version
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Camera
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
    
    // ML Kit for OCR with proper dependency resolution
    implementation("com.google.mlkit:text-recognition:16.0.0")
    
    // ML Kit document scanner - use text-recognition instead for now
    // Commented out because the official artifact is not publicly available
    // implementation("com.google.mlkit:document-scanner:16.0.0")
    
    // OpenCV - using a different library that's available in Maven repositories
    implementation("org.openpnp:opencv:4.5.1-2")
    
    // TensorFlow Lite with proper dependency management
    implementation("org.tensorflow:tensorflow-lite:2.13.0") {
        exclude(group = "io.netty")
        exclude(group = "com.google.protobuf")
    }
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0") {
        exclude(group = "io.netty")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4") {
        exclude(group = "io.netty")
        exclude(group = "com.google.protobuf")
    }
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.13.0") {
        exclude(group = "io.netty")
    }
    
    // Image processing libraries
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0") // GPU-accelerated image processing
    implementation("com.github.bumptech.glide:glide:4.16.0")  // For image loading and caching
    
    // Room for database with proper KSP configuration
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    
    // SQLCipher for encryption
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    ksp("com.google.dagger:hilt-compiler:2.48")
    
    // Charts for data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
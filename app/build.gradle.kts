/*
 * File này nằm ở: app/build.gradle.kts
 * ❗ THAY THẾ TOÀN BỘ file cũ bằng file này
 */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Dòng này ÁP DỤNG plugin Google Services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.khoaluan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.khoaluan" // ❗ SỬA TÊN GÓI CỦA BẠN
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ❗ MỚI: Bật MultiDex (Cần thiết cho các app lớn/Firebase)
        multiDexEnabled = true
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

        // ❗ MỚI: Bật Core Library Desugaring (Sửa lỗi API 26)
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // Bảo Gradle không nén file .tflite
    configurations.all {
        resolutionStrategy {
            force("org.tensorflow:tensorflow-lite:2.16.1")
        }
    }
    buildFeatures {
        viewBinding = true // Bật ViewBinding
    }
    androidResources {
        noCompress += listOf("tflite")
    }
}

dependencies {
    configurations.all {
        resolutionStrategy {
            force("org.tensorflow:tensorflow-lite:2.16.1")
            force("org.tensorflow:tensorflow-lite-api:2.16.1")
            implementation("com.google.android.gms:play-services-location:21.0.1")
        }
    }
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")

    // Android cơ bảnf
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Thư viện TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation("com.google.firebase:firebase-ml-modeldownloader-ktx:22.0.9")
    // Thư viện Firebase (BOM - Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    // 1. Firebase Authentication (Đăng nhập)
    implementation("com.google.firebase:firebase-auth-ktx")
    // 2. Firebase Firestore (CSDL cho Lịch sử & Biện pháp)
    implementation("com.google.firebase:firebase-firestore-ktx")
    // 3. Firebase Storage (Lưu ảnh lịch sử)
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    // Thư viện cho RecyclerView (Hiển thị Lịch sử)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Thư viện Glide (Tải ảnh từ link Firebase)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ❗ MỚI: Thư viện Desugaring (để sửa lỗi API 26)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}
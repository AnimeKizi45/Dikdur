plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dikdur.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dikdur.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += "task" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")

    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")

    // Yapay zekâ: yüz ve vücut noktası modelleri
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}

// MediaPipe model dosyalarını ilk derlemede otomatik indirir (src/main/assets içine)
val modelleriIndir by tasks.registering {
    val klasor = file("src/main/assets")
    val modeller = mapOf(
        "face_landmarker.task" to "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
        "pose_landmarker_lite.task" to "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task"
    )
    doLast {
        klasor.mkdirs()
        modeller.forEach { (ad, url) ->
            val hedef = File(klasor, ad)
            if (!hedef.exists()) {
                println("İndiriliyor: $ad")
                java.net.URL(url).openStream().use { girdi -> hedef.outputStream().use { girdi.copyTo(it) } }
            }
        }
    }
}
tasks.named("preBuild") { dependsOn(modelleriIndir) }

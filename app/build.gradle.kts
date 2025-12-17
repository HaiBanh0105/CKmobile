plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.banking"
    compileSdk = 36
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        applicationId = "com.example.banking"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            // pickFirsts: Nếu gặp trùng file, chỉ lấy file đầu tiên tìm thấy và bỏ qua các file sau
            pickFirsts.add("META-INF/LICENSE.md")
            pickFirsts.add("META-INF/NOTICE.md")
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Thư viện cho PinView
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-firestore:24.9.1")

    implementation("com.google.firebase:firebase-storage:20.3.0")
// FirebaseUI Firestore (cung cấp FirestoreRecyclerOptions, FirestoreRecyclerAdapter)
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")

    implementation ("androidx.camera:camera-core:1.3.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.5")
// ML Kit Vision (cung cấp InputImage)
    implementation("com.google.mlkit:vision-common:17.3.0")

    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.fragment:fragment:1.6.2")

    // Thư viện gửi Email
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    //Thư viện xử lý embedding
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")

    //màu
    implementation("com.google.android.material:material:1.12.0")

    //gg map
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.libraries.places:places:4.1.0")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")


    implementation("com.github.Dimezis:BlurView:version-1.6.6")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    val isWindows = System.getProperty("os.name").toLowerCase().contains("win")

    tasks.register("downloadFacenetModel") {
        doLast {
            file("$projectDir/src/main/assets").mkdirs()
            val facenetUrl = "https://drive.google.com/uc?export=download&id=1wglx5H9rOBEuko61msypn_rojNcm3lsr"
            val facenetFile = "$projectDir/src/main/assets/facenet.tflite"

            if (!file(facenetFile).exists()) {
                println("Downloading facenet.tflite model...")
                if (isWindows) {
                    exec {
                        commandLine("powershell", "-Command", "Invoke-WebRequest -Uri '$facenetUrl' -OutFile '$facenetFile'")
                    }
                } else {
                    exec {
                        commandLine("curl", "-L", facenetUrl, "-o", facenetFile)
                    }
                }
            }
        }
    }

    tasks.named("preBuild") {
        dependsOn("downloadFacenetModel")
    }


}


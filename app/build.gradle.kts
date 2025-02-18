plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.feri.faceguard3d"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.feri.faceguard3d"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
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

        //sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        buildFeatures {
            viewBinding = true
        }

        sourceSets["main"].jniLibs.srcDir("src/main/jniLibs")

        dependencies {

            // Android Core
            implementation("androidx.appcompat:appcompat:1.6.1")
            implementation("com.google.android.material:material:1.11.0")
            implementation("androidx.constraintlayout:constraintlayout:2.1.4")
            implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")
            implementation("androidx.activity:activity:1.8.2")

            // ML Kit Face Detection and Recognition
            implementation("com.google.mlkit:face-detection:16.1.5")
            implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")

            // CameraX
            implementation("androidx.camera:camera-core:1.3.1")
            implementation("androidx.camera:camera-camera2:1.3.1")
            implementation("androidx.camera:camera-lifecycle:1.3.1")
            implementation("androidx.camera:camera-view:1.3.1")

            // TensorFlow Lite
            implementation("org.tensorflow:tensorflow-lite:2.14.0")
            implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
            implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
            implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")

            // Room Database
            implementation("androidx.room:room-runtime:2.6.1")
            annotationProcessor("androidx.room:room-compiler:2.6.1")

            // OpenCV pentru procesare imagine
            implementation("org.opencv:opencv-android:4.8.0")
            implementation("org.bytedeco:javacv-platform:1.5.9")
            implementation("org.bytedeco:opencv:4.7.0-1.5.9")

            // ARCore pentru mapare 3D
            implementation("com.google.ar:core:1.40.0")

            // Biometric
            implementation("androidx.biometric:biometric:1.1.0")

            // Gson pentru salvare date
            implementation("com.google.code.gson:gson:2.10.1")

            // WorkManager pentru task-uri în background
            implementation("androidx.work:work-runtime:2.9.0")

            // Pentru animații și tranziții
            implementation("androidx.transition:transition:1.4.1")

            // Pentru permisiuni
            implementation("com.guolindev.permissionx:permissionx:1.7.1")

            // Testing
            testImplementation("junit:junit:4.13.2")
            androidTestImplementation("androidx.test.ext:junit:1.1.5")
            androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


            implementation("androidx.security:security-crypto:1.1.0-alpha06")

            implementation(files("${project.rootDir}/opencv-4.10.0-android-sdk/OpenCV-android-sdk/sdk/java/src"))
            implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
        }
    }
}
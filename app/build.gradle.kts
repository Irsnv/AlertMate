plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

// Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.alertmate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alertmate"
        minSdk = 28
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
//import the firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation ("com.google.firebase:firebase-auth")
    implementation ("com.google.firebase:firebase-firestore")
//scalable size unit (support for diff screen size)
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")
//implementation koin (dependency injection)
    implementation("io.insert-koin:koin-core:4.1.0")
//retrofit framework for making HTTP requests.)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
//view model (helps store and manage UI related data)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
//coil (image loading library)
    implementation("io.coil-kt:coil:2.5.0")
//swipe refresh layout
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
//asynchronous programming (can fetch data without freeze the UI)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
//more like advanced version of ListView
    implementation("androidx.recyclerview:recyclerview:1.3.0")
}
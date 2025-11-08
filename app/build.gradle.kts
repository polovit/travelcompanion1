plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")

    id("org.jetbrains.kotlin.kapt")
    id("kotlin-kapt")
}
android {
    namespace = "com.example.travelcompanion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.travelcompanion"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
        viewBinding = true
    }


    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
// Per i test locali (ExampleUnitTest.kt)
    testImplementation("junit:junit:4.13.2")
    // Per i test strumentati (ExampleInstrumentedTest.kt)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)
// WorkManager per i job periodici
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // Librerie Compose base
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3") // <-- importantissima
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
// Room (Database ORM)
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    //picassso
    implementation("com.squareup.picasso:picasso:2.8")

// Estensioni per Kotlin (DAO e coroutine)
    implementation("androidx.room:room-ktx:2.6.1")
    //maps
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Libreria Material XML (per i temi XML tipo themes.xml)
    implementation("com.google.android.material:material:1.12.0")
    // Jetpack Navigation per Compose o XML
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")

    // Libreria per Grafici
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}

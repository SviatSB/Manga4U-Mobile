plugins {
    alias(libs.plugins.android.application)

}

android {
    namespace = "com.example.mangaapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mangaapp"
        minSdk = 23
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
    }
}
configurations.all {
    exclude(group = "xpp3", module = "xpp3")
}
dependencies {

    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")
    implementation ("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation ("androidx.navigation:navigation-ui:2.5.3")

    // Retrofit для HTTP-запитів
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Для роботи з JSON
    implementation ("com.google.code.gson:gson:2.8.8")

    // ViewPager2 для навігації між формами
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    // Material Design компоненти
    implementation ("com.google.android.material:material:1.11.0")

    // Додайте ці, якщо ще немає
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")

    // CircleImageView для круглих аватарок
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
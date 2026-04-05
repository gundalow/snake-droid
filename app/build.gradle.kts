plugins {
    alias(libs.plugins.android.application)
    // alias(libs.plugins.kotlin.android)
    // alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.korge)
    alias(libs.plugins.jlleitschuh.ktlint)
    alias(libs.plugins.io.gitlab.arturbosch.detekt)
}

ktlint {
    filter {
        exclude {
            it.file.absolutePath.contains("/build/") ||
                it.file.absolutePath.contains("/app/src/main/AndroidManifest.xml")
        }
    }
}

afterEvaluate {
    tasks.findByName("ktlintAndroidMainSourceSetCheck")?.enabled = false
    tasks.findByName("ktlintCommonMainSourceSetCheck")?.enabled = false
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

korge {
    id = "dev.gundalow.snake"
    name = "Snake"
    androidSdk(compileSdk = 35, minSdk = 26, targetSdk = 35)
    targetAndroid()
    jvmMainClassName = "dev.gundalow.snake.MainKt"
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "dev.gundalow.snake"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.gundalow.snake"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // kotlinOptions {
    //    jvmTarget = "17"
    // }
    buildFeatures {
        compose = false
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.kbox2d.android)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

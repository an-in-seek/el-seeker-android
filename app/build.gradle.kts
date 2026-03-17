import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String, default: String = ""): String =
    localProps.getProperty(key, default)

android {
    namespace = "com.elseeker.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.elseeker.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val kakaoAppKey = localProp("KAKAO_APP_KEY")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")
        buildConfigField("String", "KAKAO_APP_KEY", "\"$kakaoAppKey\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${localProp("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${localProp("NAVER_CLIENT_SECRET")}\"")
        manifestPlaceholders["KAKAO_APP_KEY"] = kakaoAppKey
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://elseeker.com\"")
            isDebuggable = true
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://elseeker.com\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.webkit)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.googleid)
    implementation(libs.kakao.sdk.user)
    implementation(libs.naver.sdk.oauth)

    debugImplementation(libs.androidx.ui.tooling)
}

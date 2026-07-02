import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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
        versionCode = 3
        versionName = "1.0.2"

        val kakaoAppKey = localProp("KAKAO_APP_KEY")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProp("GOOGLE_WEB_CLIENT_ID")}\"")
        buildConfigField("String", "KAKAO_APP_KEY", "\"$kakaoAppKey\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${localProp("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${localProp("NAVER_CLIENT_SECRET")}\"")
        manifestPlaceholders["KAKAO_APP_KEY"] = kakaoAppKey
    }

    buildTypes {
        debug {
            // 기본은 운영 백엔드. 로컬 백엔드로 붙이려면 local.properties 에 DEBUG_BASE_URL 을
            // 지정한다(에뮬레이터 http://10.0.2.2:8080, 실기기는 호스트 LAN IP).
            // http 사용 시 network_security_config 에 cleartext 허용을 추가한다. (PRD §8)
            buildConfigField("String", "BASE_URL", "\"${localProp("DEBUG_BASE_URL", "https://elseeker.com")}\"")
            isDebuggable = true
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://elseeker.com\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 네이티브 디버그 기호를 AAB에 포함 → Play Console이 자동 인식.
            // 네이티브 크래시/ANR을 심볼화된 스택으로 분석 가능.
            ndk {
                debugSymbolLevel = "FULL"
            }
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // 의존성 주입
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // 네트워크 (REST API — v1 전부 네이티브, WebView 미사용)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // 이미지 로딩
    implementation(libs.coil.compose)

    // 로컬 저장 (토큰: EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)

    // 플랫폼/브랜딩
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // 소셜 로그인 SDK
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.googleid)
    implementation(libs.kakao.sdk.user)
    implementation(libs.naver.sdk.oauth)

    debugImplementation(libs.androidx.ui.tooling)
}

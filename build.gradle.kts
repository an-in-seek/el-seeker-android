buildscript {
    dependencies {
        // Hilt Gradle 플러그인(AggregateDepsTask)이 javapoet 1.13+ 의 ClassName.canonicalName() 을
        // 요구하는데, 다른 플러그인이 구버전(1.10)을 끌어와 NoSuchMethod 로 빌드가 깨진다.
        // 빌드 클래스패스에 1.13.0 을 강제한다. (Hilt issue #3386)
        classpath("com.squareup:javapoet:1.13.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

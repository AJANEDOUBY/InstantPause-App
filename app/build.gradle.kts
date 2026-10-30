import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") version "2.60.1"
    id("com.google.devtools.ksp") version "2.3.6"
}

android {
    namespace = "com.cooldown.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cooldown.app"
        minSdk = 29
        targetSdk = 37
        versionCode = 8
        versionName = "1.3.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                // ⚠️ `kotlin.**` est volontairement exclu du repackaging R8 : la stdlib
                // Kotlin est découpée en classes « multi-file » dont la hiérarchie
                // (SequencesKt__… extends SequencesKt__…) casse si R8 les renomme,
                // ce qui provoquait un IllegalAccessError au démarrage.
                packageScope = setOf("androidx.**", "kotlinx.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// ⚠️ Remplace l'ancien bloc `kotlinOptions { }` (supprimé dans AGP 9)
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose (déjà fourni par le template via le catalogue de versions)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Icônes Compose étendues (non présentes dans le template)
    implementation("androidx.compose.material:material-icons-extended")

    // === Hilt (injection de dépendances) ===
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")

    // === Room (base de données locale) ===
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // === DataStore (préférences) ===
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // === libphonenumber (normalisation E.164) ===
    implementation("com.googlecode.libphonenumber:libphonenumber:9.0.41")

    // === Lifecycle Compose ===
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    // === Tests ===
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Đọc API Key từ local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val apiKey = localProperties.getProperty("GEMINI_API_KEY") ?: "\"\""
        val senderEmail = localProperties.getProperty("SENDER_EMAIL") ?: "\"\""
        val senderPassword = localProperties.getProperty("SENDER_PASSWORD") ?: "\"\""
        
        buildConfigField("String", "GEMINI_API_KEY", apiKey)
        buildConfigField("String", "SENDER_EMAIL", senderEmail)
        buildConfigField("String", "SENDER_PASSWORD", senderPassword)
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
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Thư viện kết nối mạng siêu nhẹ và ổn định
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Thư viện đọc file .docx siêu nhẹ
    implementation("org.zwobble.mammoth:mammoth:1.5.0")
    
    // Thư viện đọc file PDF (phiên bản tối ưu cho Android)
    implementation("com.itextpdf:itextg:5.5.10")

    // Thư viện gửi Email JavaMail
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    // Thư viện Room SQLite
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
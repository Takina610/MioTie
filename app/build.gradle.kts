import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// 读取 application.properties
val appProperties = Properties().apply {
    file("${rootProject.projectDir}/application.properties").inputStream().use { load(it) }
}

// 读取 keystore.properties（如果存在）
val keystorePropertiesFile = file("${rootProject.projectDir}/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// 是否配置了正式发布签名（供 release 校验与 signingConfig 选择使用）
val hasReleaseSigning = keystoreProperties.getProperty("keystore.file", "").isNotBlank()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
    //alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.wire)
}

val sha: String? = System.getenv("GITHUB_SHA")
val isCI: String? = System.getenv("CI")
val isSelfBuild = isCI.isNullOrEmpty() || !isCI.equals("true", ignoreCase = true)
val applicationVersionCode = appProperties.getProperty("versionCode").toInt()
var applicationVersionName = appProperties.getProperty("versionName")
val isPerVersion = appProperties.getProperty("isPreRelease").toBoolean()
if (isPerVersion) {
    applicationVersionName += "-${appProperties.getProperty("preReleaseName")}.${appProperties.getProperty("preReleaseVer")}"
}
if (!isSelfBuild && !sha.isNullOrEmpty()) {
    applicationVersionName += "+${sha.substring(0, 7)}"
}

wire {
    sourcePath {
        srcDir("src/main/protos")
    }

    kotlin {
        android = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    buildToolsVersion = "36.0.0"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.takina.miotie"
        minSdk = 23
        //noinspection OldTargetApi
        targetSdk = 37
        versionCode = applicationVersionCode
        versionName = applicationVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["is_self_build"] = "$isSelfBuild"
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
    signingConfigs {
        val keystoreFile = keystoreProperties.getProperty("keystore.file", "")
        if (keystoreFile.isNotBlank()) {
            create("config") {
                storeFile = file(File(rootDir, keystoreFile))
                storePassword = keystoreProperties.getProperty("keystore.password")
                keyAlias = keystoreProperties.getProperty("keystore.key.alias")
                keyPassword = keystoreProperties.getProperty("keystore.key.password")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
        // 内置开发签名，仅供本地 debug 构建。不要依赖 AGP 自动生成的 debug.keystore：
        // 它的路径随 ANDROID_USER_HOME 或 ~/.android 变化，换环境后签名随之改变，
        // 覆盖安装失败，只能卸载重装，登录态一起丢失。
        val devKeystore = rootProject.layout.projectDirectory.file("dev.keystore").asFile
        if (devKeystore.exists()) {
            create("dev") {
                storeFile = devKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            isJniDebuggable = true
            multiDexEnabled = true
            // 本地调试用固定开发签名，保证反复覆盖安装不丢登录态
            if (signingConfigs.any { it.name == "dev" }) {
                signingConfig = signingConfigs.getByName("dev")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            multiDexEnabled = true
            // 发布包只允许正式签名，绝不回退到开发签名或 AGP 的 debug 签名。
            // 缺少 keystore.properties 时由文件末尾的校验直接失败，避免误发测试签名。
            if (signingConfigs.any { it.name == "config" }) {
                signingConfig = signingConfigs.getByName("config")
            }
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }
    composeCompiler {
        metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
        reportsDestination.set(layout.buildDirectory.dir("compose_metrics"))

        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability_configuration.txt"))
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "DebugProbesKt.bin"
        }
    }
    namespace = "com.huanchengfly.tieba.post"
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "MioTie-v${applicationVersionName}.apk"
            )
        }
    }
}

// release 必须使用正式签名。缺少 keystore.properties 时，只要本次构建涉及 release 任务就直接失败，
// 避免把开发签名或未签名包当作发布产物发出去；纯 debug 构建不受影响。
val requestedReleaseTask = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
}
if (requestedReleaseTask && !hasReleaseSigning) {
    throw GradleException(
        "Release 构建需要正式签名，但未找到 keystore.properties。" +
            "请在项目根目录配置 keystore.file / keystore.password / keystore.key.alias / keystore.key.password；" +
            "本地调试请使用 debug 构建。"
    )
}

dependencies {
    //Local Files
//    implementation fileTree(include: ["*.jar"], dir: "libs")

    implementation(libs.net.swiftzer.semver.semver)

    implementation(libs.airbnb.lottie)
    implementation(libs.airbnb.lottie.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.material.kolor)
    implementation("androidx.compose.material3:material3")

    // 悬浮底栏毛玻璃
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.kyant.capsule)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    // implementation(libs.androidx.navigation.compose)

    api(libs.wire.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.accompanist.drawablepainter)

    // Replaced deprecated Accompanist modules with official/third-party alternatives
    implementation(libs.eygraber.placeholder.material)
    implementation(libs.systemuibars.tweaker)

    implementation(libs.sketch.core)
    implementation(libs.sketch.compose)
    implementation(libs.sketch.ext.compose)
    implementation(libs.sketch.gif)
    implementation(libs.sketch.okhttp)

    implementation(libs.zoomimage.compose.sketch)

    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    runtimeOnly(libs.compose.runtime.tracing)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.material)
    implementation(libs.compose.material.navigation)
    implementation(libs.compose.material.icons.core)
    // Optional - Add full set of material icons
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.util)
//    implementation "androidx.compose.material3:material3"

    // Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugRuntimeOnly(libs.compose.ui.test.manifest)

    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.github.oaid)

    implementation(libs.org.jetbrains.annotations)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    //AndroidX
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.window)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.swiperefreshlayout)

    //Test
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestRuntimeOnly(libs.androidx.test.runner)

    //Glide
    implementation(libs.glide.core)
    ksp(libs.glide.ksp)
    implementation(libs.glide.okhttp3.integration)

    implementation(libs.google.material)

    implementation(libs.okhttp3.core)
    implementation(libs.retrofit2.core)
    implementation(libs.retrofit2.converter.wire)

    implementation(libs.google.gson)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.com.jaredrummler.colorpicker)

    implementation(libs.github.matisse)
    implementation(libs.xx.permissions)
    implementation(libs.com.gyf.immersionbar.immersionbar)

    implementation(libs.com.github.yalantis.ucrop)

    //implementation(libs.com.jakewharton.butterknife)
    //ksp(libs.com.jakewharton.butterknife.compiler)

    // ksp(libs.kotlin.metadata.jvm)
}

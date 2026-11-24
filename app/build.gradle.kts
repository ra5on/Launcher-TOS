plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release-Signatur nur über Umgebungsvariablen (Upload-Schlüssel für Play App Signing).
// Ohne sie bleibt der Release-Build (APK/AAB) unsigniert.
val releaseKeystore: String? = System.getenv("LAUNCHERTOS_KEYSTORE")?.takeIf { it.isNotBlank() }
val releasePassword: String? = System.getenv("LAUNCHERTOS_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }

android {
    namespace = "de.umstieg.launcher"
    compileSdk = 37
    defaultConfig {
        applicationId = "de.umstieg.launcher"
        minSdk = 26
        targetSdk = 37
        versionCode = 100
        versionName = "1.0.0"
    }
    signingConfigs {
        if (releaseKeystore != null && releasePassword != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = releasePassword
                keyAlias = "launchertos-upload"
                keyPassword = releasePassword
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        // Release unsigniert (signiert wird vom Inhaber), mit R8.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests.all {
            it.maxHeapSize = "1g"
            it.maxParallelForks = 1
        }
    }
    lint {
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

/** Kopiert LICENSE und THIRD_PARTY_LICENSES in die Assets (Anzeige unter „Über & Lizenzen“). */
abstract class CopyLicensesTask : DefaultTask() {
    @get:InputFiles
    abstract val licenseFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val dir = outputDir.get().asFile.resolve("licenses")
        dir.deleteRecursively()
        dir.mkdirs()
        licenseFiles.files.forEach { it.copyTo(dir.resolve(it.name), overwrite = true) }
    }
}

val copyLicenses = tasks.register<CopyLicensesTask>("copyLicenses") {
    licenseFiles.from(rootProject.file("LICENSE"), rootProject.file("THIRD_PARTY_LICENSES"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyLicenses, CopyLicensesTask::outputDir)
    }
}

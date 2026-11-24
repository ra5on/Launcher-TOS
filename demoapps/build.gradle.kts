// Demo-Apps nur für Store-Bilder (nicht Teil des Launchers): je Flavor eine eigene kleine App mit
// deutschem Namen, eigenem Icon, App-Kategorie und Demo-Benachrichtigungen. Quellen je Flavor erzeugt
// tools/generate.py. Eigene Pakete statt Aliasse, damit Benachrichtigungen, Badges und Kategorien je App
// getrennt sind.
plugins {
    alias(libs.plugins.android.application)
}

val demoApps = listOf(
    "telefon", "browser", "chat", "musik", "kamera", "galerie", "kalender", "wetter", "karten", "mail", "notizen",
    "aufgaben", "fitness", "banking", "zuhause", "podcasts", "shop", "rechner", "uhr", "dateien", "uebersetzer",
)

android {
    namespace = "de.umstieg.launcher.demo"
    compileSdk = 37
    defaultConfig {
        applicationId = "de.umstieg.launcher.demo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    flavorDimensions += "app"
    productFlavors {
        demoApps.forEach { name ->
            create(name) {
                dimension = "app"
                applicationIdSuffix = ".$name"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
    }
}

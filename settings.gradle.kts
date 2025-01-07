pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    gradlePluginPortal()
}

plugins { id("dev.kikugie.stonecutter") version "0.5" }

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    shared {
        versions( // Make sure to update .github/workflows/publish.yml when changing versions!
            "1.21.4",
            "1.21.3",
            "1.21.1",
            "1.21",
            "1.20.6",
            "1.20.4")
    }
    create(rootProject)
}

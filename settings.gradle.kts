import dev.kikugie.stonecutter.StonecutterSettings


pluginManagement.repositories {
    maven("https://maven.fabricmc.net/")
    gradlePluginPortal()
}

plugins { id("dev.kikugie.stonecutter") version "0.4.3" }

extensions.configure<StonecutterSettings> {
    kotlinController = true
    centralScript = "build.gradle.kts"
    shared {
        versions( // Make sure to update .github/workflows/publish.yml when changing versions!
            "1.21.3",
            "1.21.2",
            "1.21.1",
            "1.21",
            "1.20.6",
            "1.20.5",
            "1.20.4",
            "1.20.3",
            "1.20.2",
            "1.20.1")
    }
    create(rootProject)
}

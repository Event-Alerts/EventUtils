pluginManagement { repositories {
    maven("https://maven.kikugie.dev/releases/")
    maven("https://maven.kikugie.dev/snapshots/")
    maven("https://maven.fabricmc.net/")
    maven("https://repo.srnyx.com/snapshots/")
    gradlePluginPortal()
} }

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("dev.kikugie.loom-back-compat") version "0.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "EventUtils"

stonecutter {
    create(rootProject) {
        versions(
            "26.1.2",
            "1.21.11",
            "1.21.6",
            "1.21.5",
            "1.21.4",
            "1.21.1",
            "1.21",
            "1.20.4")
        vcsVersion = "1.21.4"
    }
}

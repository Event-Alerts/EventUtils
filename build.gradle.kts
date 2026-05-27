import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.getDefaultReplacements
import xyz.srnyx.gradlegalaxy.utility.setupJava


plugins {
    java
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("xyz.srnyx.gradle-galaxy") version "2.1.0"
}

// Get Java version
val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
stonecutter.dependency("java", java.majorVersion)

val fullVersion = "${stonecutter.current.version}-${property("mod.version").toString()}"
setupJava(JavaSetupConfig("cc.aabss", fullVersion, "Alerting for Event Alerts Minecraft events", java))

repository("https://repo.faststats.dev/releases", "https://maven.nucleoid.xyz/")
repository(Repository.SHEDANIEL, Repository.FABRIC, Repository.TERRAFORMERS, Repository.ISXANDER, Repository.MAVEN_CENTRAL, Repository.JITPACK)

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings("net.fabricmc:yarn:${property("deps.yarn_mappings")}")

    implementation("dev.faststats.metrics:fabric:0.23.0")

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("dev.isxander:yet-another-config-lib:${property("deps.yacl")}")
    modImplementation("com.terraformersmc:modmenu:${property("deps.modmenu")}")
}

// Add placeholder-api dependency if property exists
if (hasProperty("deps.placeholder_api")) dependencies.modCompileOnly("eu.pb4:placeholder-api:${property("deps.placeholder_api")}")

// Replacements for fabric.mod.json and config.json
addReplacementsTask(setOf("fabric.mod.json"), getDefaultReplacements() + mapOf(
    "mod_name" to property("mod.name").toString(),
    "mod_version" to property("mod.version").toString(),
    "deps_minecraft" to property("deps.minecraft").toString()))

base.archivesName = name

// Copy built jar to root project's build/libs
tasks.named("build") {
    doLast {
        val fileName = "${rootProject.name}-${fullVersion}.jar"
        layout.projectDirectory.dir("build/libs").asFile.listFiles()
            ?.firstOrNull { it.name == fileName }
            ?.copyTo(layout.projectDirectory.dir("../../build/libs").asFile.resolve(fileName), true)
    }
}

if (stonecutter.current.isActive) {
    loom.runConfigs.all {
        ideConfigGenerated(true)
        runDir = "../../run"
    }

    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(tasks.named("build"))
    }
}

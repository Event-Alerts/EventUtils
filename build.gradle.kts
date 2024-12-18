import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.getDefaultReplacements
import xyz.srnyx.gradlegalaxy.utility.setupJava


plugins {
    java
    id("fabric-loom") version "1.8-SNAPSHOT"
    id("xyz.srnyx.gradle-galaxy") version "1.3.2"
}

// Get Java version
val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
stonecutter.dependency("java", java.majorVersion)

setupJava("cc.aabss", "${stonecutter.current.version}-${property("mod.version").toString()}", "Alerting for Event Alerts Minecraft events", java)

repository("https://maven.shedaniel.me", "https://maven.fabricmc.net", "https://maven.terraformersmc.com/releases", "https://maven.isxander.dev/releases")
repository(Repository.MAVEN_CENTRAL, Repository.JITPACK)

dependencies {
    minecraft("com.mojang", "minecraft", property("deps.minecraft").toString())
    mappings("net.fabricmc", "yarn", property("deps.yarn_mappings").toString())
    modImplementation("net.fabricmc", "fabric-loader", property("deps.fabric_loader").toString())
    modImplementation("net.fabricmc.fabric-api", "fabric-api", property("deps.fabric_api").toString())
    modImplementation("dev.isxander", "yet-another-config-lib", property("deps.yacl").toString())

    modApi("com.terraformersmc", "modmenu", "11.0.0-beta.1")
    include(implementation("cc.aabss", "discord4j16", "8bd3efd930"))
}

// Replacements for fabric.mod.json and config.json
addReplacementsTask(setOf("fabric.mod.json"), getDefaultReplacements() + mapOf(
    "mod_name" to property("mod.name").toString(),
    "mod_version" to property("mod.version").toString(),
    "deps_minecraft" to property("deps.minecraft").toString()))

base {
    archivesName = rootProject.name
}

// Copy built jar to root project's build/libs
tasks.named("build") {
    doLast {
        val targetDir = layout.projectDirectory.dir("../../build/libs").asFile
        layout.projectDirectory.dir("build/libs").asFile.listFiles()?.forEach { it.copyTo(targetDir.resolve(it.name), true) }
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

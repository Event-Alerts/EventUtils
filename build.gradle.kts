import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.getDefaultReplacements
import xyz.srnyx.gradlegalaxy.utility.setupJava


plugins {
    java
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("xyz.srnyx.gradle-galaxy") version "1.3.3"
}

// Get Java version
val java = if (stonecutter.eval(stonecutter.current.version, ">=1.20.5")) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
stonecutter.dependency("java", java.majorVersion)

val fullVersion = "${stonecutter.current.version}-${property("mod.version").toString()}"
setupJava("cc.aabss", fullVersion, "Alerting for Event Alerts Minecraft events", java)

repository("https://maven.shedaniel.me/", "https://maven.fabricmc.net/", "https://maven.terraformersmc.com/releases/", "https://maven.isxander.dev/releases/", "https://maven.nucleoid.xyz/")
repository(Repository.MAVEN_CENTRAL, Repository.JITPACK)

dependencies {
    minecraft("com.mojang", "minecraft", property("deps.minecraft").toString())
    mappings("net.fabricmc", "yarn", property("deps.yarn_mappings").toString())

    modCompileOnly("net.fabricmc", "fabric-loader", property("deps.fabric_loader").toString())
    modCompileOnly("net.fabricmc.fabric-api", "fabric-api", property("deps.fabric_api").toString())

    modCompileOnly("dev.isxander", "yet-another-config-lib", property("deps.yacl").toString())
    modCompileOnly("com.terraformersmc", "modmenu", property("deps.modmenu").toString())

    // Discord IPC (bundle into the mod jar)
    include(implementation("com.github.jagrosh:DiscordIPC:master-SNAPSHOT")!!)
}

// Add placeholder-api dependency if property exists
if (hasProperty("deps.placeholder_api")) dependencies.modCompileOnly("eu.pb4", "placeholder-api", property("deps.placeholder_api").toString())

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

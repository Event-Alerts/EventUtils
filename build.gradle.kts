import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.getDefaultReplacements
import xyz.srnyx.gradlegalaxy.utility.setupJava

plugins {
    java
    id("fabric-loom") version "1.6.9"
    id("xyz.srnyx.gradle-galaxy") version "1.1.3"
}

var modVersion = "1.4"
setupJava("cc.aabss", "1.20.6-${modVersion}", "Alerting for Event Alerts Minecraft events", JavaVersion.VERSION_21, null)

// Replacements for fabric.mod.json
addReplacementsTask(setOf("fabric.mod.json"), getDefaultReplacements() + mapOf("mod_version" to modVersion))

// Repositories
repository(Repository.MAVEN_CENTRAL, Repository.JITPACK)
repository("https://maven.shedaniel.me", "https://maven.fabricmc.net", "https://maven.terraformersmc.com/releases")

// Dependencies
dependencies {
    minecraft("com.mojang", "minecraft", "1.20.6")
    mappings("net.fabricmc:yarn:1.20.6+build.3:v2")
    modImplementation("net.fabricmc", "fabric-loader", "0.15.11")
    modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.99.0+1.20.6")
    modApi("me.shedaniel.cloth", "cloth-config-fabric", "14.0.126")
    modApi("com.terraformersmc", "modmenu", "10.0.0-beta.1")
    include(implementation("com.github.NepNep21", "DiscordRPC4j16", "1.2.2"))
}

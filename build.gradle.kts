import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingPlatformConfig
import xyz.srnyx.gradlegalaxy.data.platforms.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.getDefaultReplacements
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import xyz.srnyx.gradlegalaxy.utility.setupJava
import xyz.srnyx.gradlegalaxy.utility.setupPublishingPlatforms


plugins {
    id("dev.kikugie.loom-back-compat")
    id("xyz.srnyx.gradle-galaxy") version "3.2.0"
    id("me.modmuss50.mod-publish-plugin") version "2.1.1"
    id("com.gradleup.shadow") version "9.6.1"
    id("dev.kikugie.fletching-table.fabric") version "0.1.0-alpha.22"
    kotlin("jvm") version "2.4.10" // For Fletching Table
    id("com.google.devtools.ksp") version "2.3.10" // For Fletching Table
}

// Properties
val modId = property("mod.id").toString()
val modName = property("mod.name").toString()
val loaderVersion = property("deps.loader").toString()
val javaUtilitiesVersion = property("library.java_utilities").toString()
val sdkVersion = property("library.sdk").toString()
val fabricApiVersion = property("deps.fabric_api").toString()
val yaclVersion = property("deps.yacl").toString()
val modMenuVersion = property("deps.modmenu").toString()
val placeholderApiVersion = if (hasProperty("deps.placeholder_api")) property("deps.placeholder_api").toString() else null

// Add local repository if using dev version
if (javaUtilitiesVersion == "dev" || sdkVersion == "dev") repository(Repository.MAVEN_LOCAL)

// Java version
val is261Plus: Boolean = sc.current.parsed >= "26.1"
val java = when {
    is261Plus -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

setupJava(JavaSetupConfig(
    group = "cc.aabss",
    description = "Alerting for Event Alerts Minecraft events",
    javaVersion = java))

// We need to let setupJava process version first then prefix with Minecraft version
val modVersion = when {
    !inGitHubWorkflow -> "0.0.0-dev"
    !inGitHubPublish -> "0.0.0-dev+$version"
    else -> version.toString()
}
version = "${sc.current.version}-$modVersion" // ex: 1.21.6-1.0.0, 1.21.4-0.0.0-dev, 1.21.11-0.0.0-dev+25fsf52

repository("https://maven.gnomecraft.net/releases/", "https://maven.nucleoid.xyz/")
repository(
    Repository.SRNYX_RELEASES, Repository.SRNYX_SNAPSHOTS,
    Repository.FABRIC, Repository.SHEDANIEL, Repository.ISXANDER,
    Repository.FASTSTATS_RELEASES, Repository.FASTSTATS_SNAPSHOTS,
    Repository.MAVEN_CENTRAL, Repository.JITPACK)

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")

    // Mappings
    if (is261Plus) {
        loomx.applyMojangMappings()
    } else {
        mappings("net.fabricmc:yarn:${property("deps.yarn_mappings").toString()}:v2")
    }

    // Library: Java Utilities
    shadowLibrary("xyz.srnyx:java-utilities:$javaUtilitiesVersion")
    // Library: Event Alerts SDK
    shadowLibrary("gg.eventalerts.sdk:http:$sdkVersion")
    shadowLibrary("gg.eventalerts.sdk:websocket:$sdkVersion")
    // Library: FastStats (1.16.1-1.17.1, 1.18-1.21.8, 1.21.9-1.21.11, 26.1-26.3)
    when {
        sc.current.version >= "1.16.1" && sc.current.version <= "1.17.1" -> "1.16.1-1.17.1"
        sc.current.version >= "1.18" && sc.current.version <= "1.21.8" -> "1.18-1.21.8"
        sc.current.version >= "1.21.9" && sc.current.version <= "1.21.11" -> "1.21.9-1.21.11"
        sc.current.version >= "26.1" && sc.current.version <= "26.3" -> "26.1-26.3"
        else -> null
    }?.let { jijLibrary("dev.faststats.metrics:fabric:${property("library.faststats")}+mc$it") }
    // Library: JDiscordIPC (https://github.com/jagrosh/DiscordIPC/pull/24/changes)
    shadowLibrary("io.github.cdagaming:DiscordIPC:${property("library.discord_ipc")}")

    // Fabric
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Mods
    modImplementation("dev.isxander:yet-another-config-lib:$yaclVersion")
    modImplementation("com.terraformersmc:modmenu:$modMenuVersion")
    placeholderApiVersion?.let { modImplementation("eu.pb4:placeholder-api:$it") }
}

base.archivesName = modName

stonecutter {
    dependencies["java"] = java.majorVersion

    // Swaps
    swaps["mod_id"] = "\"$modId\""
    swaps["mod_name"] = "\"$modName\""
    swaps["mod_version"] = "\"$modVersion\""
    swaps["mod_version_full"] = "\"$version\""
}

fletchingTable {
    fabric {
        entrypointMappings.put("modmenu", "com.terraformersmc.modmenu.api.ModMenuApi")
    }

    mixins.create("main") {
        mixin("default", "eventutils.mixins.json") {
            env("client")
        }
    }

    lang.create("main") {
        patterns.add("assets/$modId/lang/**")
    }
}

// Replacements for fabric.mod.json
addReplacementsTask(setOf("fabric.mod.json"), getDefaultReplacements() + mapOf(
    "mod_id" to modId,
    "mod_name" to modName,
    "mod_version" to modVersion,
    "deps_minecraft" to sc.current.version,
    "deps_loader" to loaderVersion,
    "deps_fabric_api" to fabricApiVersion,
    "deps_yacl" to yaclVersion,
    "deps_modmenu" to modMenuVersion))

tasks {
    jar { archiveClassifier.set("") }

    shadowJar {
        archiveClassifier.set("shadow")
        configurations.set(project.configurations.named("shadow").map { listOf(it) })
        mergeServiceFiles()

        val libsPackage = "${project.group}.$modId.libs"
        // Java Utilities
        relocate("xyz.srnyx.javautilities", "$libsPackage.javautilities")
        // Event Alerts SDK
        relocate("gg.eventalerts.sdk", "$libsPackage.eventalerts.sdk")
        relocate("com.google.errorprone", "$libsPackage.errorprone")
        relocate("com.google.gson", "$libsPackage.gson")
        relocate("org.bson", "$libsPackage.bson")
        relocate("org.java_websocket", "$libsPackage.java_websocket")
        relocate("org.slf4j", "$libsPackage.slf4j")
        // JDiscordIPC
        relocate("com.jagrosh.discordipc", "$libsPackage.discordipc")
        relocate("net.lenni0451.reflect", "$libsPackage.lenni0451.reflect")
        relocate("org.newsclub.lib.junixsocket", "$libsPackage.junixsocket")
        relocate("org.newsclub.net.unix", "$libsPackage.newsclub.unix")
    }

    remapJar {
        dependsOn(shadowJar)
        mustRunAfter(shadowJar)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
    }

    // Builds the version into a shared folder in `build/libs`
    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod and copies the jar to a shared folder"

        // loomx.modJar returns the jar task for the applied loom variant
        from(loomx.modJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs"))

        dependsOn("build")
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        generateRunConfig.set(true)
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDirectory.set(file("../../run")) // Shares the run directory between versions
    }
}

// Platform publishing
setupPublishingPlatforms(PublishingPlatformConfig(
    platforms = mapOf(PluginPlatform.MODRINTH to "ZcRRACSs"),
    minecraftVersionStart = sc.current.version,
    minecraftVersionEnd = sc.current.version,
    loaders = listOf("fabric"),
    addAnnoyingApiDependency = false,
    modrinthAction = {
        // Fabric API
        requires {
            id.set("P7dR8mSH")
            version.set(fabricApiVersion)
        }
        // YetAnotherConfigLib (YACL)
        requires {
            id.set("1eAoo2KR")
            version.set(yaclVersion)
        }
        // Text Placeholder API
        placeholderApiVersion?.let { requires {
            id.set("eXts2L7r")
            version.set(it)
        } }
        // Mod Menu
        optional {
            id.set("mOgUt4GM")
            version.set(modMenuVersion)
        }
    }))

// Register buildActive task
if (sc.current.isActive) rootProject.tasks.register("buildActive") {
    group = "build"
    description = "Builds the mod for the currently active Minecraft version"

    // Build mod
    dependsOn(tasks.named("build"))
    // Copy built jar to shared folder
    dependsOn(tasks.named("buildAndCollect"))
}

// Custom jijLibrary (modImplementation + include) and shadowLibrary (implementation + shadow) dependency configurations
fun DependencyHandler.jijLibrary(dependency: String) {
    modImplementation(dependency)
    include(dependency)
}
fun DependencyHandler.shadowLibrary(dependency: String) {
    implementation(dependency)
    shadow(dependency)
}

import xyz.srnyx.gradlegalaxy.data.config.JavaSetupConfig
import xyz.srnyx.gradlegalaxy.data.config.publishing.PublishingPlatformConfig
import xyz.srnyx.gradlegalaxy.data.platforms.PluginPlatform
import xyz.srnyx.gradlegalaxy.enums.Repository
import xyz.srnyx.gradlegalaxy.enums.repository
import xyz.srnyx.gradlegalaxy.utility.addReplacementsTask
import xyz.srnyx.gradlegalaxy.utility.getDefaultReplacements
import xyz.srnyx.gradlegalaxy.utility.setupJava
import xyz.srnyx.gradlegalaxy.utility.setupPublishingPlatforms


plugins {
    id("dev.kikugie.loom-back-compat")
    id("xyz.srnyx.gradle-galaxy") version "3.2.0"
    id("me.modmuss50.mod-publish-plugin") version "2.1.1"
}

// Properties
val modId = property("mod.id").toString()
val modName = property("mod.name").toString()
val loaderVersion = property("loader.version").toString()
val fabricApiVersion = property("deps.fabric_api").toString()
val yaclVersion = property("deps.yacl").toString()
val modMenuVersion = property("deps.modmenu").toString()
val placeholderApiVersion = if (hasProperty("deps.placeholder_api")) property("deps.placeholder_api").toString() else null

val is261Plus: Boolean = sc.eval(sc.current.version, ">=26.1")
val java = when {
    is261Plus -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

setupJava(JavaSetupConfig(
    group = "cc.aabss",
    description = "Alerting for Event Alerts Minecraft events",
    javaVersion = java))
version = "${sc.current.version}-$version" // We need to let setupJava process version first then prefix with Minecraft version

repository("https://maven.shedaniel.me/", "https://maven.terraformersmc.com/releases/", "https://maven.isxander.dev/releases/", "https://maven.nucleoid.xyz/")
repository(Repository.FABRIC, Repository.MAVEN_CENTRAL, Repository.JITPACK)

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")

    // Mappings
    if (is261Plus) {
        loomx.applyMojangMappings()
    } else {
        mappings("net.fabricmc:yarn:${property("deps.yarn_mappings").toString()}:v2")
    }

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
}

// Replacements for fabric.mod.json and config.json
addReplacementsTask(setOf("fabric.mod.json"), getDefaultReplacements() + mapOf(
    "mod_id" to modId,
    "mod_name" to property("mod.name").toString(),
    "mod_version" to version.toString(),
    "deps_minecraft" to sc.current.version,
    "deps_loader" to loaderVersion,
    "deps_fabric_api" to fabricApiVersion,
    "deps_yacl" to yaclVersion,
    "deps_modmenu" to modMenuVersion))

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDirectory.set(file("../../run")) // Shares the run directory between versions
    }
}

// Platform publishing
setupPublishingPlatforms(PublishingPlatformConfig(
    platforms = mapOf(PluginPlatform.MODRINTH to "ZcRRACSs"),
    minecraftVersionStart = sc.current.version,
    minecraftVersionEnd = sc.current.version,
    loaders = listOf("fabric", "quilt"),
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

tasks {
    jar {
        archiveClassifier.set("")
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

// Register buildActive task
if (sc.current.isActive) rootProject.tasks.register("buildActive") {
    group = "build"
    description = "Builds the mod for the currently active Minecraft version"

    // Build mod
    dependsOn(tasks.named("build"))
    // Copy built jar to shared folder
    dependsOn(tasks.named("buildAndCollect"))
}

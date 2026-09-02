import net.fabricmc.loom.task.RemapJarTask
import xyz.srnyx.gradlegalaxy.utility.inGitHubPublish
import xyz.srnyx.gradlegalaxy.utility.inGitHubWorkflow
import java.io.File
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption


plugins {
    id("dev.kikugie.loom-back-compat")
    id("xyz.srnyx.gradle-galaxy") version "56c48b7"
    id("com.gradleup.shadow") version "9.6.1"
    id("me.modmuss50.mod-publish-plugin") version "04fb4ab"

    // Fletching Table
    alias(ft.plugins.default)
    alias(ft.plugins.fabric)
    alias(ft.plugins.mixin)
    alias(ft.plugins.lang)
}

group = "cc.aabss"
description = "Alerting for Event Alerts Minecraft events"

// Properties
val modId = property("mod.id").toString()
val modName = property("mod.name").toString()
val loaderVersion = property("deps.loader").toString()
val javaUtilitiesVersion = property("library.java_utilities").toString()
val sdkVersion = property("library.sdk").toString()
val okaeriConfigsVersion = property("library.okaeri_configs").toString()
val fabricApiVersion = property("deps.fabric_api").toString()
val yaclVersion = property("deps.yacl").toString()
val modMenuVersion = property("deps.modmenu").toString()
val placeholderApiVersion = if (hasProperty("deps.placeholder_api")) property("deps.placeholder_api").toString() else null

// Java version
val is261Plus: Boolean = sc.current.parsed >= "26.1"
val javaVersionProject = when {
    is261Plus -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

// Mod version
val modVersion = when {
    !inGitHubWorkflow -> "0.0.0-snapshot"
    !inGitHubPublish -> "0.0.0-snapshot.${galaxy.java.version.get()}"
    else -> galaxy.java.version.get()
}

galaxy {
    java {
        version = "${sc.current.version}-$modVersion" // ex: 1.21.6-1.0.0, 1.21.4-0.0.0-snapshot, 1.21.11-0.0.0-snapshot.25fsf52
        javaVersion = javaVersionProject
    }

    repository {
        // Add local repository if using snapshot version
        if (javaUtilitiesVersion == "snapshot" || sdkVersion == "snapshot" || okaeriConfigsVersion == "snapshot") add(MAVEN_LOCAL)

        add(
            GNOMECRAFT_RELEASES, NUCLEOID,
            OKAERI_RELEASES, OKAERI_SNAPSHOTS,
            SRNYX_RELEASES, SRNYX_SNAPSHOTS,
            FABRIC, SHEDANIEL, ISXANDER,
            FASTSTATS_RELEASES, FASTSTATS_SNAPSHOTS,
            MAVEN_CENTRAL, JITPACK)
    }

    minecraft {
        replacementFiles = setOf("fabric.mod.json")
        replacements.putAll(replacements.get() + mapOf(
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_version" to modVersion,
            "fabric_api_id" to if (is261Plus) "fabric-api" else "fabric",
            "deps_minecraft" to sc.current.version,
            "deps_loader" to loaderVersion,
            "deps_fabric_api" to fabricApiVersion,
            "deps_yacl" to yaclVersion,
            "deps_modmenu" to modMenuVersion))

        platformPublishing {
            modrinth("ZcRRACSs") {
                file = (if (is261Plus) tasks.shadowJar else loomx.modJar)
                    .flatMap { it.archiveFile }
                    .map { it.asFile }

                // Fabric API
                requires {
                    id = "P7dR8mSH"
                    version = fabricApiVersion
                }
                // YetAnotherConfigLib (YACL)
                requires {
                    id = "1eAoo2KR"
                    version = yaclVersion
                }
                // Text Placeholder API
                placeholderApiVersion?.let { requires {
                    id = "eXts2L7r"
                    version = it
                } }
                // Mod Menu
                optional {
                    id = "mOgUt4GM"
                    version = modMenuVersion
                }
            }

            minecraftVersionStart = sc.current.version
            minecraftVersionEnd = sc.current.version
            apiTiers.add(FABRIC)
            addAnnoyingApiDependency = false
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")

    // Mappings
    loomx.applyMojangMappings()

    // Library: Java Utilities
    shadowLibrary("xyz.srnyx:java-utilities:$javaUtilitiesVersion")
    // Library: Semver4j
    shadowLibrary("org.semver4j:semver4j:${property("library.semver4j")}")
    // Library: Event Alerts SDK
    shadowLibrary("gg.eventalerts.sdk:http:$sdkVersion")
    shadowLibrary("gg.eventalerts.sdk:websocket:$sdkVersion")
    // Library: Okaeri Configs
    shadowLibrary("eu.okaeri:okaeri-configs-core:$okaeriConfigsVersion")
    shadowLibrary("eu.okaeri:okaeri-configs-json-gson:$okaeriConfigsVersion")
    shadowLibrary("eu.okaeri:okaeri-configs-serdes-commons:$okaeriConfigsVersion")
    // Library: JDiscordIPC (https://github.com/jagrosh/DiscordIPC/pull/24/changes)
    shadowLibrary("io.github.cdagaming:DiscordIPC:${property("library.discord_ipc")}")
    // Library: FastStats (1.16.1-1.17.1, 1.18-1.21.8, 1.21.9-1.21.11, 26.1-26.3)
    when {
        sc.current.parsed >= "1.16.1" && sc.current.parsed <= "1.17.1" -> "1.16.1-1.17.1"
        sc.current.parsed >= "1.18" && sc.current.parsed <= "1.21.8" -> "1.18-1.21.8"
        sc.current.parsed >= "1.21.9" && sc.current.parsed <= "1.21.11" -> "1.21.9-1.21.11"
        sc.current.parsed >= "26.1" && sc.current.parsed <= "26.3" -> "26.1-26.3"
        else -> {
            logger.warn("Unsupported Minecraft version for FastStats: ${sc.current.version}")
            null
        }
    }?.let { jijLibrary("dev.faststats.metrics:fabric:${property("library.faststats")}+mc$it") }

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
    dependencies["java"] = javaVersionProject.majorVersion

    // Swaps
    swaps["mod_id"] = "\"$modId\""
    swaps["mod_name"] = "\"$modName\""
    swaps["mod_version"] = "\"$modVersion\""
    swaps["mod_version_full"] = "\"$version\""

    // Replacements
    replacements.string(current.parsed >= "26.1") {
        replace("ClientCommandManager", "ClientCommands")
        replace("GuiGraphics", "GuiGraphicsExtractor")
        replace("net.minecraft.client.renderer.state.CameraRenderState", "net.minecraft.client.renderer.state.level.CameraRenderState")
    }
    replacements.string(current.parsed < "1.21") {
        replace("GenericMessageScreen", "GenericDirtMessageScreen")
    }
}

fletchingTable {
    fabric.configure("main") {
        entrypoint("modmenu", "com.terraformersmc.modmenu.api.ModMenuApi")
    }

    mixins.configure("main") {
        mixin("eventutils.mixins.json") {
            env("client")
        }
    }

    lang.configure("main") {}
}

tasks {
    if (is261Plus) {
        jar { archiveClassifier = "dev" }
        val includeJarsDir = layout.buildDirectory.dir("processIncludeJars").get().asFile
        shadowJar {
            archiveClassifier = ""
            dependsOn(jar, "processIncludeJars")
            doLast { JarInJarGrafter.graft(archiveFile.get().asFile, includeJarsDir) }
        }
    } else {
        jar { archiveClassifier = "" }
        shadowJar { archiveClassifier = "shadow" }
        named<RemapJarTask>("remapJar") {
            dependsOn(shadowJar)
            mustRunAfter(shadowJar)
            inputFile = shadowJar.flatMap { it.archiveFile }
        }
    }

    shadowJar {
        configurations = project.configurations.named("shadow").map { listOf(it) }
        mergeServiceFiles()

        val libsPackage = "${project.group}.$modId.libs"
        // Java Utilities
        relocate("xyz.srnyx.javautilities", "$libsPackage.javautilities")
        // Semver4j
        relocate("org.semver4j", "$libsPackage.semver4j")
        // Event Alerts SDK
        exclude("org/bson/codecs/**")
        relocate("gg.eventalerts.sdk", "$libsPackage.eventalerts.sdk")
        relocate("com.google.errorprone", "$libsPackage.errorprone")
        relocate("com.google.gson", "$libsPackage.gson")
        relocate("org.bson", "$libsPackage.bson")
        relocate("org.java_websocket", "$libsPackage.java_websocket")
        relocate("org.slf4j", "$libsPackage.slf4j")
        // Okaeri Configs
        exclude("org/jspecify/annotations/**")
        relocate("eu.okaeri", "$libsPackage.okaeri")
        // JDiscordIPC
        relocate("com.jagrosh.discordipc", "$libsPackage.discordipc")
        relocate("net.lenni0451.reflect", "$libsPackage.lenni0451.reflect")
        relocate("org.newsclub.lib.junixsocket", "$libsPackage.junixsocket")
        relocate("org.newsclub.net.unix", "$libsPackage.newsclub.unix")
    }

    // Builds the version into a shared folder in `build/libs`
    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod and copies the jar to a shared folder"

        // loomx.modJar returns the jar task for the applied loom variant
        from((if (is261Plus) shadowJar else loomx.modJar).map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs"))

        dependsOn("build")
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

    if (!is261Plus) decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        preferGradleTask = true
        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDirectory = file("../../run") // Shares the run directory between versions
    }
}

// Register tasks for active version
if (sc.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "build"
        description = "Builds the mod for the currently active Minecraft version"

        // Build mod + copy built jar to shared folder
        dependsOn(tasks.named("build"), tasks.named("buildAndCollect"))
    }

    rootProject.tasks.register("runActive") {
        group = "fabric"
        description = "Runs the mod for the currently active Minecraft version"

        // Run mod
        dependsOn(tasks.named("runClient"))
    }
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

// object required for configuration cache
object JarInJarGrafter {
    fun graft(targetJar: File, includeJarsDir: File) {
        val includeJars = includeJarsDir.listFiles { file -> file.isFile && file.extension == "jar" }?.sorted().orEmpty()

        FileSystems.newFileSystem(URI.create("jar:${targetJar.toURI()}"), emptyMap<String, Any>()).use { fs ->
            // Nested Jar-in-Jar mods
            if (includeJars.isNotEmpty()) {
                val jarsDir = fs.getPath("META-INF", "jars")
                Files.createDirectories(jarsDir)
                for (jarFile in includeJars) Files.copy(jarFile.toPath(), jarsDir.resolve(jarFile.name), StandardCopyOption.REPLACE_EXISTING)
            }

            // fabric.mod.json `jars` entries
            val fmjPath = fs.getPath("fabric.mod.json")
            @Suppress("UNCHECKED_CAST")
            val fmj = groovy.json.JsonSlurper().parse(Files.readAllBytes(fmjPath)) as MutableMap<String, Any?>
            fmj["jars"] = includeJars.map { mapOf("file" to "META-INF/jars/${it.name}") }
            Files.newOutputStream(fmjPath).use { it.write(groovy.json.JsonOutput.toJson(fmj).toByteArray()) }
        }
    }
}

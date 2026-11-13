import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.net.URLClassLoader
import java.util.zip.ZipFile

import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    idea
    java
    id("gg.essential.loom") version "1.15.50"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.gradleup.shadow") version "9.6.1"
    kotlin("jvm") version "2.3.10"
}

//Constants:

val baseGroup: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project
val modname: String by project
val version: String by project
val mcVersion: String by project
val username: String by project

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

sourceSets.main {
    java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
}

// Dependencies:

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.spongepowered.org/maven/")

    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")

    maven("https://repo.nea.moe/releases")
    maven("https://maven.notenoughupdates.org/releases")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadowModImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}


dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    implementation(kotlin("stdlib-jdk8"))
    shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // If you don't want mixins, remove these lines
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    annotationProcessor("com.google.code.gson:gson:2.14.0")
    annotationProcessor("com.google.guava:guava:33.6.0-jre")
    annotationProcessor("org.ow2.asm:asm-tree:9.10.1")
    annotationProcessor("org.ow2.asm:asm-commons:9.10.1")

    // If you don't want to log in with your real minecraft account, remove this line
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")

    shadowModImpl("org.notenoughupdates.moulconfig:legacy:4.8.0")

    shadowImpl(libs.libautoupdate)
    shadowImpl("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
}

// Minecraft configuration:
loom {
    runs {
        named("client") {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            programArgs("--tweakClass", "io.github.notenoughupdates.moulconfig.tweaker.DevelopmentResourceTweaker")
        }
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.$modid.json")
    }
    // If you don't want mixins, remove these lines
    @Suppress("UnstableApiUsage")
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}


// Tasks:

tasks.compileJava {
    dependsOn(tasks.processResources)
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set(modid)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
    }
}

tasks.processResources {
    inputs.property("mixinGroup", mixinGroup)
    inputs.property("modid", modid)
    inputs.property("modname", modname)
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("username", username)

    filesMatching(listOf("mcmod.info", "mixins.$modid.json")) {
        expand(inputs.properties)
    }

    rename("(.+_at.cfg)", "META-INF/$1")
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    inputFile.set(tasks.shadowJar.get().archiveFile)
}


tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl, shadowModImpl)
    exclude("META-INF/versions/**")

    // Relocation renames the classes named in META-INF/services, so those files have to be regenerated;
    // without this ServiceLoader still looks for the unrelocated name and finds nothing.
    mergeServiceFiles()

    // If you want to include other dependencies and shadow them, you can relocate them in here
    relocate("io.github.notenoughupdates.moulconfig", "$baseGroup.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "$baseGroup.deps.libautoupdate")
}

tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}

// MoulConfig finds its Minecraft adapter with ServiceLoader (IMinecraft.java:135 in 4.8.0), so relocating it
// also renames the interface the service file is keyed by. Check the jar users get, because a missed service
// file only shows up as a NoSuchElementException at the first config GUI call.
val moulConfigPackage = "$baseGroup.deps.moulconfig"
// Loom's remapJar is a gradle.jvm.tasks.Jar (a Zip), not a bundling.Jar, so only their common base carries archiveFile.
val moulConfigJar = tasks.named("remapJar").map { it as AbstractArchiveTask }

val verifyMoulConfigService by tasks.registering {
    group = "verification"
    description = "Checks the built jar registers and loads MoulConfig's Minecraft adapter after relocation."
    dependsOn(moulConfigJar)
    val releasedJar = moulConfigJar
    val serviceFile = "META-INF/services/$moulConfigPackage.common.IMinecraft"
    val originalServiceFile = "META-INF/services/io.github.notenoughupdates.moulconfig.common.IMinecraft"
    val adapter = "$moulConfigPackage.internal.ForgeMinecraft"
    doLast {
        val jarFile = releasedJar.get().archiveFile.get().asFile
        ZipFile(jarFile).use { jar ->
            check(jar.getEntry(originalServiceFile) == null) {
                "${jarFile.name} still has $originalServiceFile, which names a class relocation removed; add mergeServiceFiles() to tasks.shadowJar"
            }
            val entry = jar.getEntry(serviceFile)
                ?: throw GradleException("${jarFile.name} has no $serviceFile; add mergeServiceFiles() to tasks.shadowJar")
            val providers = jar.getInputStream(entry).bufferedReader().use { it.readLines() }.filter(String::isNotBlank)
            check(providers == listOf(adapter)) {
                "$serviceFile names $providers instead of [$adapter]"
            }
        }
        val thread = Thread.currentThread()
        val previousLoader = thread.contextClassLoader
        URLClassLoader(arrayOf(jarFile.toURI().toURL())).use { loader ->
            thread.contextClassLoader = loader
            try {
                val instance = Class.forName("$moulConfigPackage.common.IMinecraft", true, loader)
                    .getField("INSTANCE")
                    .get(null)
                check(instance.javaClass.name == adapter) { "IMinecraft.INSTANCE is ${instance.javaClass.name}, not $adapter" }
            } catch (e: Throwable) {
                throw GradleException("$moulConfigPackage.common.IMinecraft.INSTANCE does not load from ${jarFile.name}", e)
            } finally {
                thread.contextClassLoader = previousLoader
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyMoulConfigService)
}

tasks.assemble.get().dependsOn(tasks.remapJar)

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
}
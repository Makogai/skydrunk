plugins {
    id("fabric-loom") version "1.10.1"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"

    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.makogai"
version = "0.1.13"

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.terraformersmc.com/")      // Mod Menu
    maven("https://maven.isxander.dev/releases")    // YACL
    maven("https://maven.shedaniel.me/")
    maven("https://maven.notenoughupdates.org/releases")
    mavenCentral()
}

val shadowModImpl by configurations.creating {
    configurations.named("modImplementation").get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.5")
    mappings("net.fabricmc:yarn:1.21.5+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.119.5+1.21.5")

    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.2+kotlin.2.1.20")

    modImplementation("com.terraformersmc:modmenu:14.0.0-rc.2")
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:15.0.127")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    modImplementation("org.notenoughupdates.moulconfig:modern-1.21.5:4.1.1-beta")

    implementation("io.github.cdimascio:dotenv-java:3.0.0")
}

loom {
    runConfigs.named("client") {
        programArgs("--username", "MakogaiDev")
    }
    mixin.defaultRefmapName.set("mixins.skydrunk.refmap.json")
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.shadowJar {
    configurations = listOf(shadowModImpl)
    relocate("io.github.notenoughupdates.moulconfig", "me.makogai.skydrunk.deps.moulconfig")
}

kotlin {
    jvmToolchain(21)
}

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.terraformersmc.com/")        // Mod Menu
//        maven("https://maven.isxander.dev/releases")      // YACL
    }
}
rootProject.name = "skydrunk"

include("src:main:kotlin")
include("src:main:java")
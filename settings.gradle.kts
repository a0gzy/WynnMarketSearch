pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.essential.gg/repository/maven-public")
    }
    plugins {
        val egtVersion = "0.1.16"
        id("gg.essential.multi-version.root") version egtVersion
        id("gg.essential.multi-version.api-validation") version egtVersion
    }
}

rootProject.buildFileName = "root.gradle.kts"

listOf(
        "1.12.2-forge"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        // This is where the `build` folder and per-version overwrites will reside
        projectDir = file("versions/$version")
        // All sub-projects get configured by the same `build.gradle.kts` file, the string is relative to projectDir
        // You could use separate build files for each project, but usually that would just be duplicating lots of code
        buildFileName = "../../build.gradle.kts"
    }
}

// We use the `build.gradle.kts` file for all the sub-projects (cause that's where most the interesting stuff lives),
// so we need to use a different build file for the original root project.
rootProject.buildFileName = "root.gradle.kts"
import gg.essential.gradle.util.noServerRunConfigs

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults")
}

val modGroup: String by project
val modBaseName: String by project
val modVersion: String = "1.0.0"
group = modGroup
base.archivesName.set("$modBaseName (${platform.mcVersionStr})")
//base.archivesName.set("$modBaseName-$modVersion (${platform.mcVersionStr})")

loom {
    launchConfigs {
        getByName("client") {
            arg("--tweakClass", "gg.essential.loader.stage0.EssentialSetupTweaker")
        }
    }
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

val embed by configurations.creating
configurations.implementation.get().extendsFrom(embed)

dependencies {
    compileOnly("gg.essential:essential-$platform:4479+ge389e52d8")


    //10472+deploy-staging+g6b07567ce
    //4479+ge389e52d8

    compileOnly("org.projectlombok:lombok:1.18.20")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    embed("gg.essential:loader-launchwrapper:1.1.3")
    compileOnly("com.squareup.okhttp3:okhttp:3.14.9")

   // compileOnly("org.spongepowered:mixin:0.8.5-SNAPSHOT")
}

tasks.compileKotlin {
    kotlinOptions {
        freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn", "-Xno-param-assertions", "-Xjvm-default=all-compatibility")
    }
}

tasks.jar {
    from(embed.files.map { zipTree(it) })

    manifest.attributes(
            mapOf(
                "ModSide" to "CLIENT",
                "FMLCorePluginContainsFMLMod" to "Yes, yes it does",
                "TweakClass" to "gg.essential.loader.stage0.EssentialSetupTweaker",
                "TweakOrder" to "0",
            )
    )
}

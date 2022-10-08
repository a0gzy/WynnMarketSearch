import gg.essential.gradle.util.versionFromBuildIdAndBranch

plugins {
    kotlin("jvm") version "1.6.0" apply false
    id("gg.essential.multi-version.root")
}

//version = "1.8.5"
version = versionFromBuildIdAndBranch()

preprocess {
    val forge11202 = createNode("1.12.2-forge", 11202, "mcp")

    forge11202.link(forge11202)
}
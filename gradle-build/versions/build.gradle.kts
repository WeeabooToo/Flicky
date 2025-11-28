plugins {
    `kotlin-dsl`
}

group = "org.jire.overwatcheat.gradle_build"
version = "0.1.0"

gradlePlugin.plugins.register("overwatcheat-versions") {
    id = name
    implementationClass = "org.jire.overwatcheat.gradle_build.versions.VersionsPlugin"
}

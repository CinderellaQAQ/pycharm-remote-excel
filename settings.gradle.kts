@file:Suppress("UnstableApiUsage")

import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    }
    plugins {
        id("rpc") version "2.1.20-0.1"
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.18.1"
}

rootProject.name = "pycharm-remote-excel"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
    }
}

include("shared")
include("frontend")
include("backend")

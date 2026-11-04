import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

group = "io.github.remoteexcel"
version = "0.3.2-251"

val intellijPlatformVersion = providers.gradleProperty("intellijPlatformVersion").get()

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        pycharm(intellijPlatformVersion)
        pluginModule(implementation(project(":frontend")))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatform {
    projectName = "pycharm-remote-excel"
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251.26094.141"
            untilBuild = "251.*"
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.PyCharm, intellijPlatformVersion)
        }
    }
}

import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
group = "io.github.remoteexcel"
version = "0.3.1-251"

val intellijPlatformVersion = providers.gradleProperty("intellijPlatformVersion").get()

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

dependencies {
    intellijPlatform {
        pycharm(intellijPlatformVersion)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatform {
    projectName = "pycharm-remote-excel"

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

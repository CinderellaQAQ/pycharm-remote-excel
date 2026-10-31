import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

group = "io.github.remoteexcel"
version = "0.2.1"

val intellijPlatformVersion = providers.gradleProperty("intellijPlatformVersion").get()
val localIdePath = providers.gradleProperty("localIdePath")

plugins {
    application
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.kotlin.jvm")
    id("rpc") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
}

subprojects {
    apply(plugin = "org.jetbrains.intellij.platform.module")
    apply(plugin = "rpc")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    intellijPlatform {
        if (localIdePath.isPresent) {
            local(localIdePath.get())
        } else {
            pycharm(intellijPlatformVersion)
        }
        pluginModule(implementation(project(":shared")))
        pluginModule(implementation(project(":frontend")))
        pluginModule(implementation(project(":backend")))
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.PyCharm, intellijPlatformVersion)
        }
    }
}

plugins {
    java
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        bundledModule("intellij.platform.frontend")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

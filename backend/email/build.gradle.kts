plugins {
    id("nu.studer.rocker") version "3.0.2"
    java
}

repositories {
    jcenter()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

rocker {
    version.set("1.3.0")
    configurations {
        create("main") {
            optimize.set(true) // optional
            templateDir.set(file("src/main/rocker"))
            outputDir.set(file("src/generated/rocker"))
        }
    }
}

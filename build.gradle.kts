plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://bitbucket.org/kangarko/libraries/raw/master")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    implementation("org.mineacademy:foundation-bukkit:7.0.0") {
        exclude(group = "org.mineacademy.plugin")
        exclude(group = "com.mojang")
        exclude(group = "io.papermc.paper")
        exclude(group = "org.apache.logging.log4j")
    }
    implementation("org.mineacademy:foundation-core:7.0.0") {
        exclude(group = "org.mineacademy.plugin")
        exclude(group = "com.mojang")
        exclude(group = "io.papermc.paper")
    }

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    build {
        dependsOn(shadowJar)
    }
}

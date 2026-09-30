plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()
description = providers.gradleProperty("description").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.4.0-SNAPSHOT")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.1")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        val pluginProperties = mapOf(
            "version" to providers.gradleProperty("version").get(),
            "description" to providers.gradleProperty("description").get()
        )

        inputs.properties(pluginProperties)
        filteringCharset = "UTF-8"

        filesMatching("plugin.yml") {
            expand(pluginProperties)
        }
    }

    shadowJar {
        archiveBaseName.set("AriatusCore")
        archiveClassifier.set("")
        archiveVersion.set("")

        relocate(
            "com.zaxxer.hikari",
            "net.ariatus.project.libs.hikari"
        )
    }

    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar.get())

            groupId = providers.gradleProperty("group").get()
            artifactId = "AriatusCore"
            version = providers.gradleProperty("version").get()
        }
    }
}
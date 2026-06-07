plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.4.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
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
        filteringCharset = "UTF-8"
    }

    shadowJar {
        archiveBaseName.set("AriatusCore")
        archiveClassifier.set("")
        archiveVersion.set("")

        relocate("com.zaxxer.hikari", "net.ariatus.project.libs.hikari")
    }

    build {
        dependsOn(shadowJar)
    }
}

val packageAriatusCore by tasks.registering(Copy::class) {
    group = "ariatus"
    description = "Copies AriatusCore.jar into dist/plugin"

    dependsOn(tasks.shadowJar)

    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("dist/plugin"))

    rename { "AriatusCore.jar" }
}

val packageAriatusModules by tasks.registering(Copy::class) {
    group = "ariatus"
    description = "Copies all Ariatus module jars into dist/modules"

    dependsOn(subprojects.map { "${it.path}:build" })

    val moduleJars = subprojects.map { subproject ->
        subproject.layout.buildDirectory.dir("libs").map { libsDir ->
            libsDir.asFileTree.matching {
                include("*.jar")
                exclude("*-sources.jar")
                exclude("*-javadoc.jar")
                exclude("*-plain.jar")
            }
        }
    }

    from(moduleJars)

    into(layout.projectDirectory.dir("dist/modules"))
}

val cleanAriatusDist by tasks.registering(Delete::class) {
    group = "ariatus"
    description = "Deletes Ariatus dist folder"

    delete(layout.projectDirectory.dir("dist"))
}

tasks.register("packageAriatus") {
    group = "ariatus"
    description = "Builds AriatusCore and all modules, then copies jars into dist"

    dependsOn(cleanAriatusDist)
    dependsOn(packageAriatusCore)
    dependsOn(packageAriatusModules)
}
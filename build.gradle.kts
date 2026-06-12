plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group   = "dev.wafflycatt"
version = "1.0.0"
description = "WaffleHomes — Homes, RTP, and TPA with full Bedrock/Geyser support"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    // Paper dev bundle for 26.1.2 — replaces compileOnly paper-api
    paperweight.paperDevBundle("26.1.2.build.67-stable")

    // Floodgate API — Bedrock player detection and native forms (soft-dependency)
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding  = "UTF-8"
        options.release   = 25
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }

    // Paper 26.1+ no longer needs reobfJar — the standard jar output is Mojang-mapped
    // and works directly with the server.
}

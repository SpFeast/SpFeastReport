plugins {
    id("java")
}

group = "cn.spfeast"
version = "26.1.0"

repositories {
    mavenCentral()
    // Purpur repository
    maven("https://repo.purpurmc.org/snapshots/")
    // Paper repository fallback
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Build against the base 1.21 API for broad 1.21.x compatibility.
    compileOnly("org.purpurmc.purpur:purpur-api:1.21-R0.1-SNAPSHOT")
    compileOnly(fileTree("libs") { include("spfeastapi-*.jar") })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}


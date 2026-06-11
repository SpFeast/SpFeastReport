plugins {
    id("java")
}

group = "cn.spfeast"
version = "1.0.0"

repositories {
    mavenCentral()
    // Purpur 官方镜像仓库
    maven("https://repo.purpurmc.org/snapshots/")
    // Paper 官方镜像仓库（作为备用）
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
// 1.21 核心依赖：使用大版本最初版编译，保证 1.21 ~ 1.21.11+ 全版本完美向下兼容
    compileOnly("org.purpurmc.purpur:purpur-api:1.21-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

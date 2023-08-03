plugins {
    java
    `java-library`
    `maven-publish`
}

group = "io.github.prcraftmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("org.ow2.asm:asm:9.5")

    compileOnly("org.jetbrains:annotations:24.0.1")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.ow2.asm:asm-util:9.5")
    testImplementation("org.ow2.asm:asm-commons:9.5")
}

tasks.test {
    useJUnitPlatform()
}

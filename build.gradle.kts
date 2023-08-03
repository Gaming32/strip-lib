plugins {
    java
    `java-library`
    `maven-publish`
}

group = "io.github.prcraftmc"
version = "0.1.4"

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

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "gaming32"
            credentials(PasswordCredentials::class)

            val baseUri = "https://maven.jemnetworks.com"
            url = uri(baseUri + if (version.toString().endsWith("-SNAPSHOT")) "/snapshots" else "/releases")
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}

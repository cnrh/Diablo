plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "org.cnrh"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("net.minestom:minestom-snapshots:1f34e60ea6")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("de.articdive:jnoise-pipeline:4.1.0")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.cnrh.Main"
    }
}
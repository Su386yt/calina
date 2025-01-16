plugins {
    kotlin("jvm") version "2.0.0"
}

group = "dev.su386"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.drewnoakes:metadata-extractor:2.18.0")
}

tasks.test {
    useJUnitPlatform()
}
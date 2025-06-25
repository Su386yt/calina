import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "dev.su386"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation(compose.material3)
    implementation(compose.ui)

    testImplementation(kotlin("test"))
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("com.drewnoakes:metadata-extractor:2.18.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.12.0")
    implementation("org.apache.commons:commons-imaging:1.0.0-alpha5")
}

compose.desktop {
    application {
        mainClass = "Calina"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "calina"
            packageVersion = "1.0.0"
        }
    }
}

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "9.3.2"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
}

group = "org.snkt.partylauncher"
version = "1.1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.raphimc.net/repository/maven-public/")
}

val ktorVersion = "3.0.3"
val coroutinesVersion = "1.10.1"
val serializationVersion = "1.7.3"
val slf4jVersion = "2.0.16"
val logbackVersion = "1.5.16"
val junitVersion = "5.11.4"
val mockkVersion = "1.13.16"

dependencies {
    // Compose Multiplatform Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Ktor Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Kotlinx Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Minecraft Authentication
    implementation("net.raphimc:MinecraftAuth:5.0.2")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
}

compose.desktop {
    application {
        mainClass = "org.snkt.partylauncher.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "BeachParty Launcher"
            packageVersion = "1.1.0"
            description = "Лаунчер для нашего сервера BeachParty"
            vendor = "BeachParty Launcher"

            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "org.snkt.partylauncher"
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "org.snkt.partylauncher.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}
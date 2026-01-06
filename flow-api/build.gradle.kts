import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.ground"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val ktorVersion = "2.3.5"
val exposedVersion = "0.44.1"
val postgresqlVersion = "42.6.0"
val bcryptVersion = "0.4"

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Security
    implementation("org.mindrot:jbcrypt:$bcryptVersion")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    
    // Web3 / Blockchain
    implementation("org.web3j:core:4.10.3")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    
    // Error Tracking
    implementation("io.sentry:sentry:6.34.0")
    
    // Svix Webhooks
    implementation("com.svix:svix:1.16.0")
    implementation("org.threeten:threetenbp:1.6.8") // Required by Svix SDK
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}

application {
    mainClass.set("com.tbd.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.test {
    useJUnitPlatform()
}

// Configure Shadow JAR (fat JAR with all dependencies)
tasks.shadowJar {
    archiveBaseName.set("ground-api")
    archiveClassifier.set("")
    archiveVersion.set("")
    
    manifest {
        attributes(mapOf("Main-Class" to "com.tbd.ApplicationKt"))
    }
}

// Make shadowJar the default JAR task
tasks.build {
    dependsOn(tasks.shadowJar)
}

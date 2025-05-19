plugins {
    id("org.springframework.boot") version "3.2.5"    // latest stable (Spring Boot 3)
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}

group = "com.aus20"        
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17    // Spring Boot 3 uses Java 17+
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")         // REST APIs
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")     // JPA (Hibernate)
    implementation("org.springframework.boot:spring-boot-starter-validation")   // Bean Validation (DTOs)
    implementation("org.springframework.boot:spring-boot-starter-security")     // Security

    // PostgreSQL Driver
    runtimeOnly("org.postgresql:postgresql")

    // Kotlin support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Scheduling tasks (price monitoring background jobs)
    implementation("org.springframework.boot:spring-boot-starter")

    // JWT
    implementation("io.jsonwebtoken:jjwt:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6") // or jjwt-gson if you prefer Gson

    // Added this for WebClient and reactive programming support
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage") // Disable old JUnit 4 engine
    }
    testImplementation("io.mockk:mockk:1.13.10")
    // Validation
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("org.hibernate.validator:hibernate-validator:8.0.0.Final")

    implementation("com.google.firebase:firebase-admin:9.2.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

plugins {
    `maven-publish`
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("plugin.spring") version "1.8.0"
    kotlin("plugin.jpa") version "1.8.0"
}

apply {
    plugin("org.gradle.maven-publish")
    plugin("org.jetbrains.kotlin.jvm")
    plugin("com.github.johnrengelman.shadow")
    plugin("org.jetbrains.kotlin.plugin.spring")
    plugin("org.jetbrains.kotlin.plugin.jpa")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}


repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.hqservice.kr/repository/maven-public/")

    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("org.springframework.boot:spring-boot-starter:3.1.1")
    // https://mvnrepository.com/artifact/io.insert-koin/koin-core
    compileOnly("io.insert-koin:koin-core:3.4.2")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-jpa
    api("org.springframework.boot:spring-boot-starter-data-jpa:3.1.1")
    api("mysql:mysql-connector-java:8.0.25")
    compileOnly("kr.hqservice:hqframework-global-core:1.0.0-SNAPSHOT")
    compileOnly("kr.hqservice:hqframework-bukkit-core:1.0.0-SNAPSHOT")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.1.1")
    implementation("info.picocli:picocli:4.7.3")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("io.reactivex.rxjava3:rxjava:3.0.4")
    implementation("com.github.f4b6a3:ulid-creator:5.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.1")

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

group = "kr.chuyong"
version = "0.0.1"
description = "Spring Boot Spigot Starter"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks {
    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        artifactId = "spigot-spring-boot"
        from(components["kotlin"])
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

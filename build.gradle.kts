plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.navapbc.piqi.evaluator"
version = "0.0.1-SNAPSHOT"
description = "piqi-evaluator"

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/io.github.linuxforhealth/hl7v2-fhir-converter
    implementation("io.github.linuxforhealth:hl7v2-fhir-converter:v1.1.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.piqialliance:reference-implementation:0.1.1")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:8.4.0")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:8.4.0")
    implementation("com.navapbc.piqi:piqi-map:0.2.1")
    implementation("com.navapbc.piqi:piqi-model:0.1.1")
    implementation("org.springframework.boot:spring-boot-starter")
    //compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

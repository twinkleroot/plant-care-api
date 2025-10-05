import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// my-app 모듈에 필요한 플러그인들을 선언합니다.
plugins {
    id("org.springframework.boot") // ❗️실행 가능한 애플리케이션
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
}

group = "kr.heeblings"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation(project(":common"))
    implementation("com.google.firebase:firebase-admin:9.2.0")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

// Spring Boot의 메인 클래스 경로를 지정
springBoot {
    mainClass.set("kr.heeblings.api.MyAppApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
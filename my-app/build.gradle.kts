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
version = "0.0.4-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // AWS S3 연동을 위한 Spring Cloud AWS Starter
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.1"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")

    // 이미지 리사이징을 위한 Thumbnailator 라이브러리
    implementation("net.coobird:thumbnailator:0.4.20")

    implementation(project(":common"))
    implementation("com.google.firebase:firebase-admin:9.2.0")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

// Spring Boot의 메인 클래스 경로를 지정
springBoot {
    mainClass.set("kr.heeblings.api.MyAppApplicationKt")
}

// bootJar 태스크를 실행한 후, 그 결과물(JAR)과 .platform 폴더를 함께 묶어
// Elastic Beanstalk가 요구하는 구조의 ZIP 파일을 생성하는 'buildZip' 태스크를 정의합니다.
tasks.register<Zip>("buildZip") {
    dependsOn(tasks.bootJar) // bootJar가 먼저 실행되도록 합니다.

    // bootJar의 결과물인 JAR 파일의 이름을 'application.jar'로 변경하여 ZIP 파일의 최상위에 복사합니다.
    from(tasks.bootJar.get().archiveFile) {
        rename { "application.jar" }
    }
    // ❗️ 프로젝트 루트의 .platform 폴더를 ZIP 파일 최상위의 .platform 폴더로 복사합니다.
    from(file("${rootProject.projectDir}/.platform")) {
        into(".platform")
    }

    archiveFileName.set("heeblings-backend.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}
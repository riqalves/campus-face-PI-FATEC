plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.4"
	id("io.spring.dependency-management") version "1.1.7"
	id("jacoco")
}



group = "br.com.fatec"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {


	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// https://mvnrepository.com/artifact/com.google.firebase/firebase-admin
	implementation("com.google.firebase:firebase-admin:9.5.0")

	implementation("com.squareup.okhttp3:okhttp:4.12.0")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

	implementation("net.coobird:thumbnailator:0.4.20")



	implementation("io.jsonwebtoken:jjwt-api:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
	implementation ("com.auth0:java-jwt:4.5.0")

	implementation("com.cloudinary:cloudinary-http44:1.39.0") {
		// excluir o commons-logging
		exclude(group = "commons-logging", module = "commons-logging")
	}

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	//testes
	testImplementation("io.mockk:mockk:1.13.8")
	testImplementation("com.ninja-squad:springmockk:4.0.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configuração do JaCoCo
tasks.test {
	finalizedBy(tasks.jacocoTestReport) // Gera o relatório logo após rodar os testes
}

tasks.jacocoTestReport {
	dependsOn(tasks.test) // Garante que os testes rodem antes
	reports {
		xml.required.set(true)
		html.required.set(true) // Gera o site HTML para você tirar print
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

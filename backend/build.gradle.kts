import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.4.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.github.node-gradle.node") version "3.0.1"
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.spring") version "1.4.31"
}

group = "com.xtages"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

val frontendDir = file(projectDir).parentFile.resolve("frontend")

node {
    // This is where the package.json file and node_modules directory are located
    nodeProjectDir.set(frontendDir)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Run `npm run build` to build the optimized version of the frontend app
val buildFrontend = tasks.register<NpmTask>("buildFrontend") {
    dependsOn(tasks.npmInstall)
    npmCommand.set(listOf("run", "build"))
    inputs.dir("${frontendDir}/src")
    inputs.dir("${frontendDir}/public")
    outputs.dir("${frontendDir}/build")
}

// Copy the built frontend app from `console/frontend/build/` to
// `console/backend/src/main/resources/public` so Spring picks it up and
// serves it.
val copyFrontendToResources = tasks.register<Copy>("copyFrontendToResources") {
    dependsOn(buildFrontend)
    from(file("${frontendDir}/build"))
    val publicOutDir = "${sourceSets["main"].resources.srcDirs.first()}/public"
    into(file(publicOutDir))
}

// Make sure the `bootJar` task depends on copying the frontend app.
tasks.withType<BootJar>() {
    dependsOn(copyFrontendToResources)
}

// Clean the files under `console/backend/src/main/resources/public`.
val cleanFrontend = tasks.register<Delete>("cleanFrontend") {
    delete("${sourceSets["main"].resources.srcDirs.first()}/public")
}

// Make sure the `clean` task depends on `cleanFrontend`.
tasks.clean {
    dependsOn(cleanFrontend)
}

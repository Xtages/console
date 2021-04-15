import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import nu.studer.gradle.jooq.JooqEdition
import nu.studer.gradle.jooq.JooqGenerate

plugins {
    id("org.springframework.boot") version "2.4.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("com.github.node-gradle.node") version "3.0.1"
    id("org.openapi.generator") version "5.1.0"
    id("nu.studer.jooq") version "5.2.1"
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
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.stripe:stripe-java:20.45.0")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.5")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("javax.validation:validation-api:2.0.1.Final")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    jooqGenerator("org.postgresql:postgresql")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val frontendDir = file(projectDir).parentFile.resolve("frontend")
val apiSpecFile = file("${sourceSets["main"].resources.srcDirs.first()}/xtages-internal-api.yaml")

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("gen/main/openapi")
        }
    }
    sourceSets.all {
        languageSettings.enableLanguageFeature("InlineClasses")
    }
}

node {
    // This is where the package.json file and node_modules directory are located
    nodeProjectDir.set(frontendDir)
}

allOpen {
    annotation("javax.persistence.Entity")
}

// Generate type-safe JOOQ files based on the DB
jooq {
    version.set("3.14.7")
    edition.set(JooqEdition.OSS)
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    user = "xtages_console"
                    url = "jdbc:postgresql://localhost:5432/xtages_console"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        excludes = "databasechangelog|databasechangeloglock"
                    }
                    generate.apply {
                        isDaos = true
                        isSpringAnnotations = true
                    }
                    target.apply {
                        packageName = "xtages.console.query"
                        directory = "gen/main/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

tasks.withType<JooqGenerate> {
    // make jOOQ task participate in incremental builds
    allInputsDeclared.set(true)

    // make jOOQ task participate in build caching
    outputs.cacheIf { true }
}

sourceSets.main {
    java.srcDirs("gen/main/kotlin")
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
tasks.withType<BootJar> {
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

// Generate Base classes and models from the openapi model
tasks.openApiGenerate {
    validateSpec.set(true)
    generatorName.set("kotlin-spring")
    inputSpec.set(apiSpecFile.toString())
    outputDir.set("$projectDir/gen")
    apiPackage.set("xtages.console.controller.api")
    modelPackage.set("xtages.console.controller.api.model")
    generateApiDocumentation.set(false)
    generateApiTests.set(false)
    generateModelDocumentation.set(false)
    generateModelTests.set(false)
    configOptions.set(
        mapOf(
            "sourceFolder" to "main/openapi",
            "apiSuffix" to "ApiControllerBase",
            "serializationLibrary" to "jackson",
            "interfaceOnly" to true.toString(),
            "enumPropertyNaming" to "UPPERCASE"
        )
    )
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

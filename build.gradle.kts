import java.util.Properties
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    antlr
    kotlin("jvm") version "1.4.21-2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.virtlink"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // Platform
    implementation      (kotlin("stdlib-jdk8"))
    compileOnly         ("com.google.code.findbugs:jsr305:3.0.2")

    // CLI
    implementation      ("com.github.ajalt:clikt:2.8.0")

    // Parsing
    antlr               ("org.antlr:antlr4:4.9")

    // Logging
    implementation      ("org.slf4j:slf4j-api:1.7.30")
    implementation      ("ch.qos.logback:logback-classic:1.2.3")
    implementation      ("io.github.microutils:kotlin-logging:1.8.3")

    // Testing
    testImplementation  ("org.junit.jupiter:junit-jupiter:5.7.0")
}

tasks {
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    application {
        mainClassName = "com.virtlink.gocolonize.MainKt"
    }
    withType<KotlinCompile> {
        dependsOn(generateGrammarSource)
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        useJUnitPlatform()
    }
    val createProperties by registering {
        dependsOn("processResources")
        doLast {
            val revision = "git describe --always".runCommand()
            val fullRevision = "git log -n1 --format=%H".runCommand()
            File("$buildDir/resources/main/version.properties").writer().use { w ->
                val p = Properties()
                p["version"] = project.version.toString()
                p["revision"] = revision
                p["full-revision"] = fullRevision
                p["build-time"] = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                p.store(w, "Version information")
            }
        }
    }

    classes {
        dependsOn(createProperties)
    }

    jar {
        manifest {
            attributes(mapOf(
                "Main-Class" to application.mainClassName
            ))
        }
    }

    generateGrammarSource {
        maxHeapSize = "64m"
        arguments = arguments + listOf("-visitor", "-long-messages", "-package", "com.virtlink.gocolonize.parser")
        outputDirectory = file("${project.buildDir}/generated-src/antlr/main/com/virtlink/gocolonize/parser")
    }

    named<ShadowJar>("shadowJar") {
        minimize()
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

sourceSets.main {
    java.srcDirs("${project.buildDir}/generated-src/antlr/main")
}

fun String.runCommand(workingDir: File = file("./")): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText().trim()
}

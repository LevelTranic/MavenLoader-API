import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    id("java")
    idea
    `maven-publish`
}

group = "one.tranic"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven-central-asia.storage-download.googleapis.com/maven2/")
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}


val targetJavaVersion = 17
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    withSourcesJar()
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

dependencies {
    implementation("org.apache.maven:maven-resolver-provider:3.9.9")
    compileOnly("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.22")
    compileOnly("org.apache.maven.resolver:maven-resolver-transport-http:1.9.22")
    compileOnly("org.jetbrains:annotations:24.0.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(javadocJar.get())
        }
    }
    repositories {
        maven {
            name = "repsy"
            url = uri("https://repo.repsy.io/mvn/rdb/default")
            credentials {
                username = project.findProperty("repsyUsername") as String? ?: System.getenv("REPSY_USERNAME")
                password = project.findProperty("repsyPassword") as String? ?: System.getenv("REPSY_PASSWORD")
            }
        }
    }
}

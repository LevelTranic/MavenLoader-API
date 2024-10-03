import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`

plugins {
    `java-library`
    idea
    `maven-publish`
}

group = "one.tranic"
version = "1.0.3.1"

repositories {
    maven("https://maven-central-asia.storage-download.googleapis.com/maven2/")
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
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

val apiAndDocs: Configuration by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

configurations.api {
    extendsFrom(apiAndDocs)
}

dependencies {
    api("org.jetbrains:annotations:24.0.0")

    api("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    api("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    api("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
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

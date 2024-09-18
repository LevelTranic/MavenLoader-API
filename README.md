# MavenLoader-API

## Usage (Gradle Kotlin DSL)
```kotlin
repositories {
    maven("https://repo.repsy.io/mvn/rdb/default") {
        name = "tranic-repo"
    }
}

dependencies {
    compileOnly("one.tranic:maven-loader-api:1.0-SNAPSHOT")
}
```
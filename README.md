# MavenLoader-API

We have already added the central repository and sonatype by default, no need to add them further.

Example in: [MavenLoader-Example](https://github.com/LevelTranic/MavenLoader-Example)

## Import Dependencies

### Gradle Groovy
```groovy
repositories {
    maven {
        url = "https://repo.repsy.io/mvn/rdb/default"
        name = "tranic-repo"
    }
}

dependencies {
    compileOnly 'one.tranic:maven-loader-api:1.0.1'
}

```

### Gradle Kotlin DSL
```kotlin
repositories {
    maven("https://repo.repsy.io/mvn/rdb/default") {
        name = "tranic-repo"
    }
}

dependencies {
    compileOnly("one.tranic:maven-loader-api:1.0.1")
}
```

### Maven
```xml
    <repositories>
        <repository>
            <id>tranic-repo</id>
            <url>https://repo.repsy.io/mvn/rdb/default</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>one.tranic</groupId>
            <artifactId>maven-loader-api</artifactId>
            <version>1.0.1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

## Usage
### Velocity Main Class
#### Java
```java
@Plugin(
    id = "my-plugin", 
    name = "MyPlugin", 
    version = BuildConstants.VERSION, 
    dependencies = {@Dependency(id = "maven-loader")}
)
public class MyPlugin {
    
}
```

#### Kotlin
```kotlin
@Plugin(
    id = "my-plugin",
    name = "MyPlugin",
    version = BuildConstants.VERSION,
    dependencies = [Dependency(id = "maven-loader")]
)
class MavenLoader {
}
```

### In loader
#### Java
```java
try {
    MavenLibraryResolver resolver = new MavenLibraryResolver();
    resolver.addRepository("https://repo.maven.apache.org/maven2", "central");
    resolver.addDependency("org.jooq:jooq:3.17.7");
    new org.jooq.util.xml.jaxb.Catalog();
} catch (Exception e) {
    throw new RuntimeException(e);
}

// or
try {
    MavenLibraryResolver resolver = new MavenLibraryResolver();
    resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());
    resolver.addDependency(new Dependency(new DefaultArtifact("org.jooq:jooq:3.17.7"), null));
    new org.jooq.util.xml.jaxb.Catalog();
} catch (Exception e) {
    throw new RuntimeException(e);
}
```

#### Kotlin
```kotlin
try {
    val resolver: MavenLibraryResolver = MavenLibraryResolver()
    resolver.addRepository("https://repo.maven.apache.org/maven2", "central")
    resolver.addDependency("org.jooq:jooq:3.17.7")
    org.jooq.util.xml.jaxb.Catalog()
} catch (e: Exception) {
    throw RuntimeException(e)
}

// or
try {
    val resolver: MavenLibraryResolver = MavenLibraryResolver()
    resolver.addRepository(RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build())
    resolver.addDependency(Dependency(DefaultArtifact("org.jooq:jooq:3.17.7"), null))
    org.jooq.util.xml.jaxb.Catalog()
} catch (e: Exception) {
    throw RuntimeException(e)
}
```

## Shadow
You need to exclude all dependencies below to prevent shadow from packaging them in your plugin:

- one.tranic:maven-loader-api
- org.apache.maven:maven-resolver-provider
- org.apache.maven.resolver:maven-resolver-connector-basic
- org.apache.maven.resolver:maven-resolver-transport-http
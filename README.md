# MavenLoader-API

We have already added the central repository and sonatype by default, no need to add them further.

Example in: [MavenLoader-Example](https://github.com/LevelTranic/MavenLoader-Example)

## Import Dependencies
**No longer recommend this method, please see [Usage](#Usage).**

### Gradle Groovy
```groovy
repositories {
    maven {
        url = "https://repo.repsy.io/mvn/rdb/default"
        name = "tranic-repo"
    }
}

dependencies {
    compileOnly 'one.tranic:maven-loader-api:1.0.3.1'
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
    compileOnly("one.tranic:maven-loader-api:1.0.3.1")
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
            <version>1.0.3.1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

## Usage

### Set hard dependence
#### Velocity (Java)
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

#### Velocity (Kotlin)
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

#### Spigot
`plugin.yml`
```yaml
name: ExamplePlugin
version: '1.0-SNAPSHOT'
main: com.example.Main
api-version: '1.18'
load: STARTUP
authors: [ "Me" ]
folia-supported: true
depend:
  - MavenLoader
```


### In resources (new)
MavenLoader `1.2-SNAPSHOT` introduced a new loading mechanism.

Now you don't need to choose the method in `In loader`. 
You only need to create a `maven.yml` in `src/main/resources`, 
like this

```yaml
repository:
  - https://jitpack.io
dependency:
  - org.jooq:jooq:3.17.7
  - com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4
  # Simple-YAML should not be added, as MavenLoaderAPI already includes that dependency since 1.2-SNAPSHOT.
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

## Links
- [Javadoc](https://javadoc.tranic.one/maven-loader)
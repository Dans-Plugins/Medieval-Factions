# Using Medieval Factions as a Dependency

This guide explains how to reference the Medieval Factions plugin JAR in your Maven or Gradle project.

## Automatic Publishing

Upon each release (including draft releases), a GitHub Action automatically builds the plugin JAR and publishes it to GitHub Packages. This allows developers to extend the plugin by referencing it in their build files.

## Adding the Dependency

### Gradle

Add the GitHub Packages repository and the dependency to your `build.gradle`:

```gradle
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Dans-Plugins/Medieval-Factions")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation 'com.dansplugins:medieval-factions:VERSION:all'
}
```

### Maven

Add the GitHub Packages repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Dans-Plugins/Medieval-Factions</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.dansplugins</groupId>
        <artifactId>medieval-factions</artifactId>
        <version>VERSION</version>
        <classifier>all</classifier>
    </dependency>
</dependencies>
```

## Authentication

To access GitHub Packages, you need to authenticate with a GitHub token.

### For Gradle

Create a `gradle.properties` file in your project root or in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_GITHUB_TOKEN
```

Or set environment variables:
```bash
export GITHUB_ACTOR=YOUR_GITHUB_USERNAME
export GITHUB_TOKEN=YOUR_GITHUB_TOKEN
```

### For Maven

Add your GitHub credentials to `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

## Creating a GitHub Token

1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate a new token with `read:packages` scope
3. Use this token as your password/token in the configuration above

## Notes

- Replace `VERSION` with the specific version you want to use (e.g., `5.7.0-alpha-1`)
- The token must have at least `read:packages` permission
- For CI/CD environments, use the `GITHUB_TOKEN` secret which is automatically provided
- Two artifacts are published for each release:
  - `medieval-factions-{version}.jar` - The standard JAR (without dependencies)
  - `medieval-factions-{version}-all.jar` - The shadowJar with all dependencies included (recommended for most use cases)

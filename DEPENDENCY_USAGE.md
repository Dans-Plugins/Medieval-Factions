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

## Registering faction role permissions

An add-on can add its own faction role permission. Register it once when the add-on enables:

```kotlin
val medievalFactions = server.pluginManager.getPlugin("MedievalFactions") as MedievalFactions
val permissions = medievalFactions.factionPermissions
permissions.addPermissionType(
    permissions.wrapSimplePermission("MY_ADDON_ACTION", "Use My Add-on", false)
)
// Later, to check it for a member:
val permission = permissions.parse("MY_ADDON_ACTION")!!
val allowed = faction.getRole(playerId)?.hasPermission(faction, permission) == true
```

The permission then appears in `/faction role view` and can be set with `/faction role setpermission`. It is stored in the faction's roles like any built-in permission, so a permission meant only for an external tool reading the database (#1796) needs nothing more than this registration.

Existing factions do not need to be updated. A role with no setting of its own resolves a registered permission as follows (#1683):

1. The **Owner** role is granted it, together with `SET_ROLE_PERMISSION(...)` for it, so each faction's owner can use it at once and grant it to other roles.
2. Every other role takes the faction's default for it, if one is set, and otherwise the default passed to `wrapSimplePermission` (`false` above).

A setting saved on a role always wins, so an Owner role that was explicitly denied the permission stays denied. The Owner role is the role named `Owner` (ignoring case), the same rule `/faction admin setleader` uses; factions store no separate owner role id, so a renamed Owner role is treated like any other role. Medieval Factions' own permissions are not affected: the Owner role's grants for them come from the role template as before.

## Notes

- Replace `VERSION` with the specific version you want to use (e.g., `5.7.0-alpha-1`)
- The token must have at least `read:packages` permission
- For CI/CD environments, use the `GITHUB_TOKEN` secret which is automatically provided
- The published artifact is the shadowJar with all dependencies included: `medieval-factions-{version}-all.jar`

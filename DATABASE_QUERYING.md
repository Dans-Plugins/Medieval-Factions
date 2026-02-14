# Medieval Factions - Database Querying Guide

This guide explains how to query the Medieval Factions database directly, including when the server is running in a Docker container on a VM.

## Table of Contents
- [Default Database (H2)](#default-database-h2)
- [Database File Location](#database-file-location)
- [Connecting to the H2 Database](#connecting-to-the-h2-database)
  - [Using the H2 Shell (CLI)](#using-the-h2-shell-cli)
  - [Using the H2 Web Console](#using-the-h2-web-console)
  - [Using a SQL Client (e.g. DBeaver)](#using-a-sql-client-eg-dbeaver)
- [Querying in a Docker Container](#querying-in-a-docker-container)
  - [Option 1: Query from the Host via the Volume Mount](#option-1-query-from-the-host-via-the-volume-mount)
  - [Option 2: Query from Inside the Container](#option-2-query-from-inside-the-container)
- [Querying on a Remote VM](#querying-on-a-remote-vm)
- [Using MySQL or PostgreSQL Instead](#using-mysql-or-postgresql-instead)
- [Database Schema Reference](#database-schema-reference)
- [Example Queries](#example-queries)

## Default Database (H2)

By default, Medieval Factions uses an embedded [H2](https://www.h2database.com/) database. The default JDBC URL in `config.yml` is:

```yaml
database:
  url: 'jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false'
  dialect: 'H2'
  username: 'sa'
  password: ''
```

Key settings in the connection URL:
- **`AUTO_SERVER=true`** — Enables H2's [automatic mixed mode](https://www.h2database.com/html/features.html#auto_mixed_mode), which starts a TCP server alongside the embedded database. This allows multiple processes to connect to the same database simultaneously, which is what makes direct querying possible while the server is running.
- **`MODE=MYSQL`** — H2 runs in MySQL compatibility mode.
- **`DATABASE_TO_UPPER=false`** — Table and column names are case-sensitive and stored in lowercase.

## Database File Location

The H2 database is stored as a file relative to the server's working directory. By default, this creates:

| Setup | Database File Path |
|-------|-------------------|
| Standalone server | `<server_directory>/medieval_factions_db.mv.db` |
| Docker (default compose.yml) | `/testmcserver/medieval_factions_db.mv.db` inside the container, mounted to `./testmcserver/medieval_factions_db.mv.db` on the host |

## Connecting to the H2 Database

### Using the H2 Shell (CLI)

1. Download the H2 database JAR from [h2database.com](https://www.h2database.com/html/download.html) or [Maven Central](https://repo1.maven.org/maven2/com/h2database/h2/2.1.214/h2-2.1.214.jar).
2. Run the H2 Shell:

```bash
java -cp h2-2.1.214.jar org.h2.tools.Shell \
  -url "jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false" \
  -user sa \
  -password ""
```

> **Important:** Run this command from the same directory that contains the `medieval_factions_db.mv.db` file. The `AUTO_SERVER=true` flag must be included in the URL so that H2 connects to the already-running database instance rather than trying to lock the file.

### Using the H2 Web Console

H2 includes a built-in web-based console for browsing and querying the database:

```bash
java -jar h2-2.1.214.jar -web -webAllowOthers -webPort 8082
```

Then open `http://localhost:8082` in your browser and enter the JDBC URL, username, and password from above.

> **Note:** The `-webAllowOthers` flag allows connections from other machines. Only use this in trusted network environments.

### Using a SQL Client (e.g. DBeaver)

Most SQL clients support H2 via JDBC. In your SQL client:

1. Create a new H2 connection.
2. Set the JDBC URL to: `jdbc:h2:<path_to_server>/medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false`
3. Set the username to `sa` and leave the password empty (unless you changed it in `config.yml`).

## Querying in a Docker Container

When running the Minecraft server in a Docker container (using the provided `compose.yml` or a project like [open-mc-server-infrastructure](https://github.com/dmccoystephenson/open-mc-server-infrastructure)), the database file is inside the container but accessible through the volume mount.

### Option 1: Query from the Host via the Volume Mount

The `compose.yml` mounts `./testmcserver` on the host to `/testmcserver` in the container:

```yaml
volumes:
  - ./testmcserver:/testmcserver
```

This means the database file is available on the host at `./testmcserver/medieval_factions_db.mv.db`.

**While the server is running**, you can connect from the host using the H2 Shell or any SQL client:

```bash
java -cp h2-2.1.214.jar org.h2.tools.Shell \
  -url "jdbc:h2:./testmcserver/medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false" \
  -user sa \
  -password ""
```

> **Note:** Because `AUTO_SERVER=true` is set, H2 will automatically connect through the TCP server that the running Minecraft server started. You must include `AUTO_SERVER=true` in your connection URL.

**While the server is stopped**, you can connect without `AUTO_SERVER=true`:

```bash
java -cp h2-2.1.214.jar org.h2.tools.Shell \
  -url "jdbc:h2:./testmcserver/medieval_factions_db;MODE=MYSQL;DATABASE_TO_UPPER=false" \
  -user sa \
  -password ""
```

### Option 2: Query from Inside the Container

You can exec into the running container and query the database from there:

```bash
# Enter the container
docker exec -it mf-test-mc-server bash

# Download the H2 JAR (if not already present)
wget -O /tmp/h2.jar https://repo1.maven.org/maven2/com/h2database/h2/2.1.214/h2-2.1.214.jar

# Connect to the database
java -cp /tmp/h2.jar org.h2.tools.Shell \
  -url "jdbc:h2:/testmcserver/medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false" \
  -user sa \
  -password ""
```

You can now run SQL queries interactively. Type `quit` to exit.

## Querying on a Remote VM

If your Minecraft server is running in a Docker container on a remote VM, you have several options:

### Option A: SSH Tunnel + H2 Web Console

1. Start the H2 Web Console inside the container or on the VM host (see [Using the H2 Web Console](#using-the-h2-web-console)).
2. Create an SSH tunnel from your local machine to the VM:

```bash
ssh -L 8082:localhost:8082 user@your-vm-ip
```

3. Open `http://localhost:8082` in your local browser and connect using the JDBC URL.

### Option B: SSH + CLI

SSH into the VM and use the H2 Shell directly on the host (via the volume mount) or inside the container:

```bash
ssh user@your-vm-ip
cd /path/to/server
java -cp h2-2.1.214.jar org.h2.tools.Shell \
  -url "jdbc:h2:./testmcserver/medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false" \
  -user sa \
  -password ""
```

### Option C: Use MySQL or PostgreSQL

For easier remote access, consider switching to MySQL or PostgreSQL, which natively support remote network connections. See [Using MySQL or PostgreSQL Instead](#using-mysql-or-postgresql-instead).

## Using MySQL or PostgreSQL Instead

If you need frequent or remote database access, switching to MySQL or PostgreSQL may be more convenient than H2. Update your `config.yml`:

**MySQL:**
```yaml
database:
  url: 'jdbc:mysql://localhost:3306/medievalfactions'
  dialect: 'MySQL'
  username: 'your_username'
  password: 'your_password'
```

**PostgreSQL:**
```yaml
database:
  url: 'jdbc:postgresql://localhost:5432/medievalfactions'
  dialect: 'PostgreSQL'
  username: 'your_username'
  password: 'your_password'
```

When running in Docker, you can add a database service to your `compose.yml` and connect to it from the Minecraft server container using Docker networking (e.g., `jdbc:mysql://db:3306/medievalfactions`).

## Database Schema Reference

Medieval Factions uses the following tables:

| Table | Description |
|-------|-------------|
| `mf_faction` | Faction data including name, description, flags (JSON), home location, bonus power, roles (JSON) |
| `mf_player` | Player data including power, bypass status, chat channel, name |
| `mf_faction_member` | Maps players to factions with their role |
| `mf_faction_invite` | Pending faction invitations |
| `mf_faction_application` | Pending faction join applications |
| `mf_faction_chat_member` | Chat channel membership |
| `mf_law` | Faction laws with ordering |
| `mf_faction_relationship` | Faction relationships (ally, at war, vassal, liege) |
| `mf_claimed_chunk` | Territory claims by world, chunk x, and chunk z |
| `mf_gate` | Gate structures with coordinates and status |
| `mf_locked_block` | Player-locked blocks/containers |
| `mf_locked_block_accessor` | Access list for locked blocks |
| `mf_player_interaction_status` | Player interaction state |
| `mf_gate_creation_context` | In-progress gate creation state |
| `mf_chat_channel_message` | Chat message history |
| `mf_duel` | Active duels |
| `mf_duel_invite` | Pending duel invitations |
| `mf_schema_history` | Flyway migration tracking (internal) |

## Example Queries

### List All Factions
```sql
SELECT id, name, description, bonus_power FROM mf_faction;
```

### List All Players and Their Power
```sql
SELECT id, name, power FROM mf_player ORDER BY power DESC;
```

### List Faction Members with Faction Names
```sql
SELECT f.name AS faction_name, p.name AS player_name, m.role_id
FROM mf_faction_member m
JOIN mf_faction f ON m.faction_id = f.id
JOIN mf_player p ON m.player_id = p.id
ORDER BY f.name, p.name;
```

### Count Claimed Chunks per Faction
```sql
SELECT f.name, COUNT(*) AS claimed_chunks
FROM mf_claimed_chunk c
JOIN mf_faction f ON c.faction_id = f.id
GROUP BY f.name
ORDER BY claimed_chunks DESC;
```

### List Faction Relationships
```sql
SELECT f1.name AS faction, f2.name AS target, r.type
FROM mf_faction_relationship r
JOIN mf_faction f1 ON r.faction_id = f1.id
JOIN mf_faction f2 ON r.target_id = f2.id
ORDER BY f1.name;
```

### List Faction Laws
```sql
SELECT f.name AS faction_name, l.number, l.text
FROM mf_law l
JOIN mf_faction f ON l.faction_id = f.id
ORDER BY f.name, l.number;
```

### Find a Player's Faction
```sql
SELECT p.name AS player_name, f.name AS faction_name
FROM mf_faction_member m
JOIN mf_player p ON m.player_id = p.id
JOIN mf_faction f ON m.faction_id = f.id
WHERE p.name = 'PlayerName';
```

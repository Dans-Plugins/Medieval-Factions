# Medieval Factions - Storage Migration Guide

This guide explains how to migrate data between storage backends in Medieval Factions.

## Table of Contents
- [Overview](#overview)
- [Before You Begin](#before-you-begin)
- [Migration Methods](#migration-methods)
  - [Automated Migration (Recommended)](#automated-migration-recommended)
  - [Manual Migration (Alternative/Legacy Method)](#manual-migration-alternativelegacy-method)
- [Troubleshooting](#troubleshooting)

## Overview

Medieval Factions supports two storage backends:
1. **Database Storage** - Uses SQL databases (H2, MySQL, MariaDB, PostgreSQL)
2. **JSON Storage** - Uses JSON files stored on disk

You may want to migrate between these backends for various reasons:
- Switching from JSON to Database for better performance on larger servers
- Switching from Database to JSON for simpler deployment or easier backups
- Moving to a different database system

## Before You Begin

**⚠️ CRITICAL: Always backup your data before migration!**

### Backup Checklist
- [ ] Stop your Minecraft server
- [ ] If using database storage, create a database backup
- [ ] If using JSON storage, copy the entire storage directory
- [ ] Keep a copy of your `config.yml`
- [ ] Document your current plugin version

### System Requirements
- The migration command can run while server is online (but server restart required after migration to switch backends)
- Sufficient disk space for both source and target storage
- Read/write permissions for storage directories
- Database credentials (if migrating to/from database)

## Migration Methods

### Automated Migration (Recommended)

**Command:** `/mf migrate <type>`

Medieval Factions now provides a migration command that allows operators to transfer data between storage backends without manual intervention. **This is the recommended approach.**

#### Using the Migrate Command

**Permission Required:** `mf.migrate`

**Syntax:**
```
/mf migrate toJson      - Migrate from database to JSON storage
/mf migrate toDatabase  - Migrate from JSON to database storage
```

**Important Notes:**
- **Always backup your data** before running any migration command
- The migration runs asynchronously and may take several minutes for large datasets
- The server does NOT need to be stopped to run the migration
- **Chat message limit**: When migrating to JSON, only the most recent 1000 messages per faction are retained to prevent file bloat. If you have more messages in your database, older messages will not be migrated.
- After successful migration, you must:
  1. Stop the server
  2. Update `storage.type` in config.yml to match the new storage backend
  3. Restart the server

**Example Migration from Database to JSON:**
1. Ensure you have a database backup
2. Run command: `/mf migrate toJson`
3. Wait for success message showing how many items were migrated
4. Stop the server
5. Edit config.yml: `storage.type: 'json'`
6. Start the server

**Example Migration from JSON to Database:**
1. Ensure you have backed up your JSON files
2. Run command: `/mf migrate toDatabase`
3. Wait for success message showing how many items were migrated
4. Stop the server
5. Edit config.yml: `storage.type: 'database'`
6. Start the server

The migration command will:
- Initialize both source and target storage systems
- Copy all data including players, factions, claims, laws, relationships, etc.
- Validate the migration was successful
- Report the number of items migrated and time taken

### Manual Migration (Alternative/Legacy Method)

**Note:** The `/mf migrate` command (above) is the recommended approach. This manual method is provided as an alternative for advanced users or troubleshooting.

If you prefer not to use the migration command or need to perform custom data manipulation, you can manually switch storage backends. **Warning:** This approach requires manual data transfer and may result in data loss if not done carefully.

#### From Database to JSON

1. **Backup your database**
   ```bash
   # For H2 database (default)
   cp medieval_factions_db.mv.db medieval_factions_db.mv.db.backup
   
   # For MySQL/MariaDB
   mysqldump -u username -p database_name > backup.sql
   ```

2. **Stop your server**
   ```bash
   ./stop.sh  # or your stop command
   ```

3. **Update config.yml**
   ```yaml
   storage:
     type: 'json'  # Change from 'database' to 'json'
     json:
       path: './medieval_factions_data'
   ```

4. **Start your server**
   - The plugin will initialize with JSON storage
   - **Note:** This will create empty JSON files - your database data is NOT automatically migrated using this manual method

5. **Manual Data Transfer Required**
   - This manual approach does NOT transfer your data automatically
   - Use the `/mf migrate toJson` command (recommended) or write custom scripts to transfer data
   - Your data will remain in the database but won't be accessible when using JSON storage
   - See the "Automated Migration" section above for the recommended migration approach

#### From JSON to Database

1. **Backup your JSON files**
   ```bash
   cp -r medieval_factions_data medieval_factions_data.backup
   ```

2. **Stop your server**

3. **Configure database in config.yml**
   ```yaml
   storage:
     type: 'database'
   database:
     url: 'jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false'
     dialect: 'H2'
     username: 'sa'
     password: ''
   ```

4. **Start your server**
   - The plugin will initialize database storage
   - **Note:** This will create empty database tables - your JSON data is NOT automatically migrated using this manual method

5. **Manual Data Transfer Required**
   - This manual approach does NOT transfer your data automatically
   - Use the `/mf migrate toDatabase` command (recommended) or write custom scripts to transfer data
   - Your data will remain in JSON files but won't be accessible when using database storage
   - See the "Automated Migration" section above for the recommended migration approach

## Troubleshooting

### Common Issues

#### "Permission denied" when accessing JSON storage
**Solution:** Ensure the server process has read/write permissions for the storage directory:
```bash
chmod -R 755 medieval_factions_data
chown -R minecraft:minecraft medieval_factions_data
```

#### "Failed to connect to database"
**Solution:** 
- Verify database credentials in config.yml
- Ensure database server is running
- Check firewall rules for database connections

#### "JSON validation failed"
**Solution:**
- Check server logs for specific validation errors
- Ensure JSON files haven't been manually edited incorrectly
- Restore from backup if files are corrupted

#### Data appears to be missing after migration
**Solution:**
- Verify you're using the correct storage type in config.yml
- Check that source data files/database are intact
- Ensure backup was created before migration
- If automated migration was used, check migration logs

### Getting Help

If you encounter issues during migration:

1. **Check the logs** - Located in `plugins/MedievalFactions/logs/`
2. **Search existing issues** - [GitHub Issues](https://github.com/Dans-Plugins/Medieval-Factions/issues)
3. **Ask on Discord** - [Support Discord](https://discord.gg/xXtuAQ2)
4. **Create a bug report** - [New Issue](https://github.com/Dans-Plugins/Medieval-Factions/issues/new/choose)

When asking for help, provide:
- Plugin version
- Server version (Paper, Spigot, etc.)
- Source and target storage types
- Relevant error messages from logs
- Steps taken before the issue occurred

## Best Practices

1. **Test on a copy first** - Use a development/test server to test migration
2. **Backup regularly** - Maintain regular backups regardless of storage type
3. **Monitor after migration** - Watch for any issues for 24-48 hours after migrating
4. **Document changes** - Keep notes on what was changed and when
5. **Plan downtime** - Inform players about planned migration downtime

## Version Compatibility

- Storage migration is supported in Medieval Factions 5.7.2+
- Earlier versions do not support JSON storage
- When upgrading, follow the upgrade guide before attempting storage migration

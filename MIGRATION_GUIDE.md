# Medieval Factions - Storage Migration Guide

This guide explains how to migrate data between storage backends in Medieval Factions.

## Table of Contents
- [Overview](#overview)
- [Before You Begin](#before-you-begin)
- [Migration Methods](#migration-methods)
  - [Manual Migration](#manual-migration)
  - [Automated Migration (Future)](#automated-migration-future)
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
- Server must be stopped during migration
- Sufficient disk space for both source and target storage
- Read/write permissions for storage directories
- Database credentials (if migrating to/from database)

## Migration Methods

### Manual Migration

Currently, migration between storage backends requires manual steps. Follow these instructions carefully:

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
   - **Note:** This will create empty JSON files - your database data is NOT automatically migrated

5. **Manual Data Transfer Required**
   - Internal migration classes exist in the codebase but are not exposed as user-facing commands
   - You must manually transfer data between storage types or wait for future versions with migration commands
   - Your data will remain in the database but won't be accessible when using JSON storage
   - Contact support or check the Discord/GitHub for community tools or scripts

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
   - **Note:** This will create empty database tables - your JSON data is NOT automatically migrated

5. **Manual Data Transfer Required**
   - Internal migration classes exist in the codebase but are not exposed as user-facing commands
   - You must manually transfer data between storage types or wait for future versions with migration commands
   - Your data will remain in JSON files but won't be accessible when using database storage
   - Contact support or check the Discord/GitHub for community tools or scripts

### Automated Migration (Future)

**Status: Not Yet Available**

While internal migration components (DatabaseToJsonMigrator and JsonToDatabaseMigrator) exist in the codebase, they are **not exposed as stable, user-facing commands** and should not be used directly by server administrators.

Future versions may include:
- Admin commands to trigger migrations (`/mf migrate toJson` or `/mf migrate toDatabase`)
- Automatic migration when storage type is changed
- Validation of migrated data
- Rollback capabilities

Until explicit migration commands are officially documented and released, **manual data transfer is required** using the procedures described in the [Manual Migration](#manual-migration) section above.

Check the plugin's GitHub repository or Discord server for updates on automated migration availability.

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

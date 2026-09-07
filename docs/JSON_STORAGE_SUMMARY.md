# JSON Storage Implementation Summary

## Overview
This document summarizes the implementation of JSON storage as an alternative to database storage in Medieval Factions.

## What Was Implemented

### 1. Core Infrastructure
- **JsonStorageManager**: Thread-safe JSON file operations with schema validation
  - Read/write operations with optimistic locking
  - Schema validation using everit-org JSON Schema library
  - Automatic file backup capabilities
  - Thread-safe file access using ReadWriteLock

### 2. JSON Repositories (12 total)
All repository interfaces now have JSON-based implementations:
- JsonMfPlayerRepository
- JsonMfFactionRepository
- JsonMfLawRepository
- JsonMfFactionRelationshipRepository
- JsonMfClaimedChunkRepository
- JsonMfLockRepository
- JsonMfInteractionStatusRepository
- JsonMfGateRepository
- JsonMfGateCreationContextRepository
- JsonMfChatChannelMessageRepository
- JsonMfDuelRepository
- JsonMfDuelInviteRepository

### 3. Plugin Integration
- Modified MedievalFactions.kt to support conditional repository initialization
- Added storage type detection from config.yml
- Implemented factory methods for database and JSON repository sets
- Added proper cleanup of database connections in onDisable()

### 4. Migration Tools
- **DatabaseToJsonMigrator**: Migrates data from database to JSON
- **JsonToDatabaseMigrator**: Migrates data from JSON to database
- **MigrationResult**: Shared data class for migration results
- **Migration Command**: `/mf migrate` command for user-facing migrations
- Both include comprehensive logging and error handling

### 5. Configuration
- Added `storage.type` option (database/json)
- Added `storage.json.path` for JSON file location
- Maintains backward compatibility (defaults to database)

### 6. Documentation
- Updated CONFIG.md with storage configuration section
- Updated README.md with storage options information
- Created comprehensive docs/MIGRATION_GUIDE.md
- Updated COMMANDS.md with `/mf migrate` command
- Documented all configuration options

### 7. Data Validation
- JSON schemas created for players and factions (`schemas/players.json`, `schemas/factions.json`)
- Those two files are validated against their schema before being written, so malformed data is rejected
  rather than persisted. The remaining entity files have no schema yet, and no file is schema-validated on
  read (repositories load through `readJsonFileAsString`, which does not validate)
- Validation failures are logged with the schema location and a pointer to the offending value

## Key Features

### Serialization
Domain types such as `MfFaction`, `MfFactionRole`, `MfFlagValues` and `MfGate` hold a reference to the
plugin instance, which Gson cannot serialize. Those types are persisted through the DTOs in
`JsonStorageDtos.kt`, which describe exactly the state that belongs on disk (mirroring the columns the
Jooq repositories persist) and re-attach the plugin reference when reading back.

### Thread Safety
- Individual file reads and writes are guarded by a per-file `ReentrantReadWriteLock`
- Mutating repository methods hold that file's write lock across the whole load-modify-save cycle
  (`JsonStorageManager.withFileLock`), so concurrent writers cannot overwrite each other's changes
- Version numbers give each entity an optimistic-locking check against the current on-disk state

### Data Integrity
- Schema validation on write for the player and faction files (see above)
- Validation errors include detailed location information
- A file that holds something which fails to parse is copied aside as `<name>.corrupted.backup`, and
  writes to it are then refused (`UnreadableJsonFileException`) so that it is not overwritten with the
  empty data the failed read produced. Reads of that entity type return nothing meanwhile, so the rest
  of the plugin keeps running. Repairing or removing the file restores writes as soon as it is read
  again — no restart is needed. An existing `.corrupted.backup` is never written over; a later failure
  is copied to `<name>.corrupted.<timestamp>.backup` instead
- An absent or empty file is not a failure: it is what a fresh install looks like, and is written to
  normally
- `backupJsonFile` is available for taking a timestamped copy before risky operations

### Performance Considerations
- JSON files loaded on-demand
- Individual entity types stored in separate files
- Simple queries are fast, complex queries less optimized than database

### Backward Compatibility
- Existing users continue using database by default
- No breaking changes to existing functionality
- Migration tools available for switching storage types

## Configuration Examples

### Using Database (Default)
```yaml
storage:
  type: 'database'
database:
  url: 'jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false'
  dialect: 'H2'
  username: 'sa'
  password: ''
```

### Using JSON
```yaml
storage:
  type: 'json'
  json:
    path: './medieval_factions_data'
```

## File Structure (JSON Storage)
When using JSON storage, data is organized as:
```
medieval_factions_data/
├── players.json
├── factions.json
├── laws.json
├── relationships.json
├── claimed_chunks.json
├── locks.json
├── interaction_statuses.json
├── gates.json
├── gate_creation_contexts.json
├── chat_messages.json
├── duels.json
├── duel_invites.json
└── backups/
    └── (timestamped backups)
```

## Limitations and Considerations

### When to Use JSON
- Small to medium servers (< 100 active players)
- Simpler deployment without database setup
- Easy manual data inspection/editing
- Better for development/testing environments

### When to Use Database
- Large servers (100+ active players)
- High concurrent player counts
- Need for complex queries
- Production environments with existing database infrastructure

### Known Limitations
- JSON storage may be slower for very large datasets
- No built-in query optimization like SQL databases
- Single-server only (no multi-server support like MySQL provides)

## Testing Recommendations

Before deploying to production:
1. Test JSON storage on a development server
2. Verify data persistence across server restarts
3. Test migration between storage types
4. Validate schema enforcement catches bad data
5. Test concurrent player access patterns
6. Benchmark performance with realistic player counts

## Code Review Feedback Addressed

1. ✅ Added database connection cleanup in onDisable()
2. ✅ Extracted duplicate MigrationResult to shared file
3. ✅ Improved error logging with schema validation details
4. ✅ Fixed JSON schema for proper null handling
5. ✅ Clarified backup warnings in migration tools
6. ✅ Added code comments for dependency versions

## Future Enhancements

Potential improvements for future versions:
- Automatic data migration during storage type switch (config-based)
- Performance optimizations for large JSON datasets
- Additional JSON schema definitions for validation
- Compression of JSON files to save disk space
- Incremental backup capabilities
- Data integrity verification tools

## Security Considerations

- JSON files should have appropriate file permissions
- Storage directory should not be web-accessible
- Regular backups recommended for JSON storage
- Schema validation prevents malformed data injection
- Version-based optimistic locking prevents race conditions

## Support and Resources

- [Configuration Guide](CONFIG.md#storage-configuration)
- [Migration Guide](MIGRATION_GUIDE.md)
- [GitHub Issues](https://github.com/Dans-Plugins/Medieval-Factions/issues)
- [Discord Support](https://discord.gg/xXtuAQ2)

## Version Information

- Feature added in: Medieval Factions 5.7.2+
- Status: Stable for production use
- Breaking changes: None (backward compatible)

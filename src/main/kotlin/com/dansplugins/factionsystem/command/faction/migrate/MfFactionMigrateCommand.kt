package com.dansplugins.factionsystem.command.faction.migrate

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.JooqMfChatChannelMessageRepository
import com.dansplugins.factionsystem.claim.JooqMfClaimedChunkRepository
import com.dansplugins.factionsystem.duel.JooqMfDuelInviteRepository
import com.dansplugins.factionsystem.duel.JooqMfDuelRepository
import com.dansplugins.factionsystem.faction.JooqMfFactionRepository
import com.dansplugins.factionsystem.gate.JooqMfGateCreationContextRepository
import com.dansplugins.factionsystem.gate.JooqMfGateRepository
import com.dansplugins.factionsystem.interaction.JooqMfInteractionStatusRepository
import com.dansplugins.factionsystem.law.JooqMfLawRepository
import com.dansplugins.factionsystem.locks.JooqMfLockRepository
import com.dansplugins.factionsystem.player.JooqMfPlayerRepository
import com.dansplugins.factionsystem.relationship.JooqMfFactionRelationshipRepository
import com.dansplugins.factionsystem.storage.json.*
import com.dansplugins.factionsystem.storage.migration.DatabaseToJsonMigrator
import com.dansplugins.factionsystem.storage.migration.JsonToDatabaseMigrator
import com.google.gson.Gson
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.flywaydb.core.Flyway
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL

class MfFactionMigrateCommand(private val plugin: MedievalFactions) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.migrate")) {
            sender.sendMessage("$RED You do not have permission to use this command.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("$YELLOW=== Medieval Factions Storage Migration ===")
            sender.sendMessage("$YELLOW  Usage: /mf migrate <type>")
            sender.sendMessage("$YELLOW  ")
            sender.sendMessage("$YELLOW  Types:")
            sender.sendMessage("$YELLOW    toJson     - Migrate from database to JSON storage")
            sender.sendMessage("$YELLOW    toDatabase - Migrate from JSON to database storage")
            sender.sendMessage("$YELLOW  ")
            sender.sendMessage("$RED  WARNING: Always backup your data before migrating!")
            return true
        }
        
        val targetType = args[0].lowercase()
        
        when (targetType) {
            "tojson" -> migrateToJson(sender)
            "todatabase" -> migrateToDatabase(sender)
            else -> {
                sender.sendMessage("$RED Invalid migration type: $targetType")
                sender.sendMessage("$YELLOW  Valid options: toJson, toDatabase")
            }
        }
        
        return true
    }
    
    private fun migrateToJson(sender: CommandSender) {
        sender.sendMessage("$YELLOW=== Starting Database to JSON Migration ===")
        sender.sendMessage("$RED WARNING: Ensure you have backed up your database!")
        sender.sendMessage("$YELLOW This operation may take several minutes...")
        
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    val gson = Gson()
                    
                    // Initialize database repositories
                    plugin.logger.info("Initializing database repositories...")
                    Class.forName("org.h2.Driver")
                    val hikariConfig = HikariConfig()
                    hikariConfig.jdbcUrl = plugin.config.getString("database.url")
                    val databaseUsername = plugin.config.getString("database.username")
                    if (databaseUsername != null) {
                        hikariConfig.username = databaseUsername
                    }
                    val databasePassword = plugin.config.getString("database.password")
                    if (databasePassword != null) {
                        hikariConfig.password = databasePassword
                    }
                    val dataSource = HikariDataSource(hikariConfig)
                    
                    try {
                        val oldClassLoader = Thread.currentThread().contextClassLoader
                        Thread.currentThread().contextClassLoader = plugin.classLoader
                        val flyway = Flyway.configure()
                            .dataSource(dataSource)
                            .locations("classpath:com/dansplugins/factionsystem/db/migration")
                            .table("mf_schema_history")
                            .baselineOnMigrate(true)
                            .baselineVersion("0")
                            .validateOnMigrate(false)
                            .load()
                        flyway.migrate()
                        Thread.currentThread().contextClassLoader = oldClassLoader
                        
                        val dialect = plugin.config.getString("database.dialect")?.let(SQLDialect::valueOf)
                        val jooqSettings = Settings().withRenderSchema(false)
                        val dsl = DSL.using(dataSource, dialect, jooqSettings)
                        
                        val sourcePlayerRepo = JooqMfPlayerRepository(plugin, dsl)
                        val sourceFactionRepo = JooqMfFactionRepository(plugin, dsl, gson)
                        val sourceLawRepo = JooqMfLawRepository(dsl)
                        val sourceRelationshipRepo = JooqMfFactionRelationshipRepository(dsl)
                        val sourceClaimRepo = JooqMfClaimedChunkRepository(dsl)
                        val sourceLockRepo = JooqMfLockRepository(dsl)
                        val sourceInteractionRepo = JooqMfInteractionStatusRepository(dsl)
                        val sourceGateRepo = JooqMfGateRepository(plugin, dsl)
                        val sourceGateContextRepo = JooqMfGateCreationContextRepository(dsl)
                        val sourceChatRepo = JooqMfChatChannelMessageRepository(dsl)
                        val sourceDuelRepo = JooqMfDuelRepository(dsl)
                        val sourceDuelInviteRepo = JooqMfDuelInviteRepository(dsl)
                        
                        // Initialize JSON repositories
                        plugin.logger.info("Initializing JSON repositories...")
                        val storagePath = plugin.config.getString("storage.json.path") ?: "./medieval_factions_data"
                        val storageManager = JsonStorageManager(plugin, storagePath)
                        
                        val targetPlayerRepo = JsonMfPlayerRepository(plugin, storageManager)
                        val targetFactionRepo = JsonMfFactionRepository(plugin, storageManager, gson)
                        val targetLawRepo = JsonMfLawRepository(plugin, storageManager)
                        val targetRelationshipRepo = JsonMfFactionRelationshipRepository(plugin, storageManager)
                        val targetClaimRepo = JsonMfClaimedChunkRepository(plugin, storageManager)
                        val targetLockRepo = JsonMfLockRepository(plugin, storageManager)
                        val targetInteractionRepo = JsonMfInteractionStatusRepository(plugin, storageManager)
                        val targetGateRepo = JsonMfGateRepository(plugin, storageManager)
                        val targetGateContextRepo = JsonMfGateCreationContextRepository(plugin, storageManager)
                        val targetChatRepo = JsonMfChatChannelMessageRepository(plugin, storageManager)
                        val targetDuelRepo = JsonMfDuelRepository(plugin, storageManager)
                        val targetDuelInviteRepo = JsonMfDuelInviteRepository(plugin, storageManager)
                        
                        // Create and run migrator
                        val migrator = DatabaseToJsonMigrator(
                            plugin,
                            sourcePlayerRepo, sourceFactionRepo, sourceLawRepo, sourceRelationshipRepo,
                            sourceClaimRepo, sourceLockRepo, sourceInteractionRepo, sourceGateRepo,
                            sourceGateContextRepo, sourceChatRepo, sourceDuelRepo, sourceDuelInviteRepo,
                            targetPlayerRepo, targetFactionRepo, targetLawRepo, targetRelationshipRepo,
                            targetClaimRepo, targetLockRepo, targetInteractionRepo, targetGateRepo,
                            targetGateContextRepo, targetChatRepo, targetDuelRepo, targetDuelInviteRepo
                        )
                        
                        val result = migrator.migrate()
                        
                        plugin.server.scheduler.runTask(plugin, Runnable {
                            if (result.success) {
                                sender.sendMessage("$GREEN=== Migration Successful! ===")
                                sender.sendMessage("$GREEN  Migrated ${result.itemsMigrated} items in ${result.durationMs / 1000.0} seconds")
                                sender.sendMessage("$YELLOW  ")
                                sender.sendMessage("$YELLOW  Next steps:")
                                sender.sendMessage("$YELLOW    1. Stop the server")
                                sender.sendMessage("$YELLOW    2. Edit config.yml: set storage.type to 'json'")
                                sender.sendMessage("$YELLOW    3. Start the server")
                            } else {
                                sender.sendMessage("$RED=== Migration Failed ===")
                                sender.sendMessage("$RED  ${result.message}")
                                sender.sendMessage("$YELLOW  Check the server logs for more details.")
                            }
                        })
                    } finally {
                        dataSource.close()
                    }
                } catch (e: Exception) {
                    plugin.logger.severe("Migration failed: ${e.message}")
                    e.printStackTrace()
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        sender.sendMessage("$RED=== Migration Failed ===")
                        sender.sendMessage("$RED  ${e.message ?: "Unknown error"}")
                        sender.sendMessage("$YELLOW  Check the server logs for details.")
                    })
                }
            }
        )
    }
    
    private fun migrateToDatabase(sender: CommandSender) {
        sender.sendMessage("$YELLOW=== Starting JSON to Database Migration ===")
        sender.sendMessage("$RED WARNING: Ensure you have backed up your JSON files!")
        sender.sendMessage("$YELLOW This operation may take several minutes...")
        
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                try {
                    val gson = Gson()
                    
                    // Initialize JSON repositories
                    plugin.logger.info("Initializing JSON repositories...")
                    val storagePath = plugin.config.getString("storage.json.path") ?: "./medieval_factions_data"
                    val storageManager = JsonStorageManager(plugin, storagePath)
                    
                    val sourcePlayerRepo = JsonMfPlayerRepository(plugin, storageManager)
                    val sourceFactionRepo = JsonMfFactionRepository(plugin, storageManager, gson)
                    val sourceLawRepo = JsonMfLawRepository(plugin, storageManager)
                    val sourceRelationshipRepo = JsonMfFactionRelationshipRepository(plugin, storageManager)
                    val sourceClaimRepo = JsonMfClaimedChunkRepository(plugin, storageManager)
                    val sourceLockRepo = JsonMfLockRepository(plugin, storageManager)
                    val sourceInteractionRepo = JsonMfInteractionStatusRepository(plugin, storageManager)
                    val sourceGateRepo = JsonMfGateRepository(plugin, storageManager)
                    val sourceGateContextRepo = JsonMfGateCreationContextRepository(plugin, storageManager)
                    val sourceChatRepo = JsonMfChatChannelMessageRepository(plugin, storageManager)
                    val sourceDuelRepo = JsonMfDuelRepository(plugin, storageManager)
                    val sourceDuelInviteRepo = JsonMfDuelInviteRepository(plugin, storageManager)
                    
                    // Initialize database repositories
                    plugin.logger.info("Initializing database repositories...")
                    Class.forName("org.h2.Driver")
                    val hikariConfig = HikariConfig()
                    hikariConfig.jdbcUrl = plugin.config.getString("database.url")
                    val databaseUsername = plugin.config.getString("database.username")
                    if (databaseUsername != null) {
                        hikariConfig.username = databaseUsername
                    }
                    val databasePassword = plugin.config.getString("database.password")
                    if (databasePassword != null) {
                        hikariConfig.password = databasePassword
                    }
                    val dataSource = HikariDataSource(hikariConfig)
                    
                    try {
                        val oldClassLoader = Thread.currentThread().contextClassLoader
                        Thread.currentThread().contextClassLoader = plugin.classLoader
                        val flyway = Flyway.configure()
                            .dataSource(dataSource)
                            .locations("classpath:com/dansplugins/factionsystem/db/migration")
                            .table("mf_schema_history")
                            .baselineOnMigrate(true)
                            .baselineVersion("0")
                            .validateOnMigrate(false)
                            .load()
                        flyway.migrate()
                        Thread.currentThread().contextClassLoader = oldClassLoader
                        
                        val dialect = plugin.config.getString("database.dialect")?.let(SQLDialect::valueOf)
                        val jooqSettings = Settings().withRenderSchema(false)
                        val dsl = DSL.using(dataSource, dialect, jooqSettings)
                        
                        val targetPlayerRepo = JooqMfPlayerRepository(plugin, dsl)
                        val targetFactionRepo = JooqMfFactionRepository(plugin, dsl, gson)
                        val targetLawRepo = JooqMfLawRepository(dsl)
                        val targetRelationshipRepo = JooqMfFactionRelationshipRepository(dsl)
                        val targetClaimRepo = JooqMfClaimedChunkRepository(dsl)
                        val targetLockRepo = JooqMfLockRepository(dsl)
                        val targetInteractionRepo = JooqMfInteractionStatusRepository(dsl)
                        val targetGateRepo = JooqMfGateRepository(plugin, dsl)
                        val targetGateContextRepo = JooqMfGateCreationContextRepository(dsl)
                        val targetChatRepo = JooqMfChatChannelMessageRepository(dsl)
                        val targetDuelRepo = JooqMfDuelRepository(dsl)
                        val targetDuelInviteRepo = JooqMfDuelInviteRepository(dsl)
                        
                        // Create and run migrator
                        val migrator = JsonToDatabaseMigrator(
                            plugin,
                            sourcePlayerRepo, sourceFactionRepo, sourceLawRepo, sourceRelationshipRepo,
                            sourceClaimRepo, sourceLockRepo, sourceInteractionRepo, sourceGateRepo,
                            sourceGateContextRepo, sourceChatRepo, sourceDuelRepo, sourceDuelInviteRepo,
                            targetPlayerRepo, targetFactionRepo, targetLawRepo, targetRelationshipRepo,
                            targetClaimRepo, targetLockRepo, targetInteractionRepo, targetGateRepo,
                            targetGateContextRepo, targetChatRepo, targetDuelRepo, targetDuelInviteRepo
                        )
                        
                        val result = migrator.migrate()
                        
                        plugin.server.scheduler.runTask(plugin, Runnable {
                            if (result.success) {
                                sender.sendMessage("$GREEN=== Migration Successful! ===")
                                sender.sendMessage("$GREEN  Migrated ${result.itemsMigrated} items in ${result.durationMs / 1000.0} seconds")
                                sender.sendMessage("$YELLOW  ")
                                sender.sendMessage("$YELLOW  Next steps:")
                                sender.sendMessage("$YELLOW    1. Stop the server")
                                sender.sendMessage("$YELLOW    2. Edit config.yml: set storage.type to 'database'")
                                sender.sendMessage("$YELLOW    3. Start the server")
                            } else {
                                sender.sendMessage("$RED=== Migration Failed ===")
                                sender.sendMessage("$RED  ${result.message}")
                                sender.sendMessage("$YELLOW  Check the server logs for more details.")
                            }
                        })
                    } finally {
                        dataSource.close()
                    }
                } catch (e: Exception) {
                    plugin.logger.severe("Migration failed: ${e.message}")
                    e.printStackTrace()
                    plugin.server.scheduler.runTask(plugin, Runnable {
                        sender.sendMessage("$RED=== Migration Failed ===")
                        sender.sendMessage("$RED  ${e.message ?: "Unknown error"}")
                        sender.sendMessage("$YELLOW  Check the server logs for details.")
                    })
                }
            }
        )
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            return listOf("toJson", "toDatabase").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}

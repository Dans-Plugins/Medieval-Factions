package com.dansplugins.factionsystem

import com.dansplugins.factionsystem.claim.JooqMfClaimedChunkRepository
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunkRepository
import com.dansplugins.factionsystem.command.MedievalFactionsCommand
import com.dansplugins.factionsystem.command.accessors.MfAccessorsCommand
import com.dansplugins.factionsystem.command.faction.MfFactionCommand
import com.dansplugins.factionsystem.command.gate.MfGateCommand
import com.dansplugins.factionsystem.command.lock.MfLockCommand
import com.dansplugins.factionsystem.command.power.MfPowerCommand
import com.dansplugins.factionsystem.command.unlock.MfUnlockCommand
import com.dansplugins.factionsystem.faction.JooqMfFactionRepository
import com.dansplugins.factionsystem.faction.MfFactionRepository
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.faction.permission.MfFactionPermission
import com.dansplugins.factionsystem.faction.permission.MfFactionPermissionSerializer
import com.dansplugins.factionsystem.gate.*
import com.dansplugins.factionsystem.gate.MfGateStatus.CLOSING
import com.dansplugins.factionsystem.gate.MfGateStatus.OPENING
import com.dansplugins.factionsystem.interaction.JooqMfInteractionStatusRepository
import com.dansplugins.factionsystem.interaction.MfInteractionService
import com.dansplugins.factionsystem.interaction.MfInteractionStatusRepository
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.law.JooqMfLawRepository
import com.dansplugins.factionsystem.law.MfLawRepository
import com.dansplugins.factionsystem.law.MfLawService
import com.dansplugins.factionsystem.listener.*
import com.dansplugins.factionsystem.locks.JooqMfLockRepository
import com.dansplugins.factionsystem.locks.MfLockRepository
import com.dansplugins.factionsystem.locks.MfLockService
import com.dansplugins.factionsystem.locks.MfRpkLockService
import com.dansplugins.factionsystem.notification.MfNotificationService
import com.dansplugins.factionsystem.notification.mailboxes.MailboxesNotificationService
import com.dansplugins.factionsystem.notification.noop.NoOpNotificationService
import com.dansplugins.factionsystem.notification.rpkit.RpkNotificationService
import com.dansplugins.factionsystem.player.JooqMfPlayerRepository
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.player.MfPlayerRepository
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.relationship.JooqMfFactionRelationshipRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipRepository
import com.dansplugins.factionsystem.relationship.MfFactionRelationshipService
import com.dansplugins.factionsystem.service.Services
import com.google.gson.GsonBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.flywaydb.core.Flyway
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import java.time.LocalTime
import java.util.*
import javax.sql.DataSource

class MedievalFactions : JavaPlugin() {

    private lateinit var dataSource: DataSource

    lateinit var flags: MfFlags
    lateinit var services: Services
    lateinit var language: Language

    override fun onEnable() {
        saveDefaultConfig()

        language = Language(ResourceBundle.getBundle("lang", Locale.forLanguageTag(config.getString("language"))))
        Metrics(this, 8929)

        Class.forName("org.h2.Driver")
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = config.getString("database.url")
        val databaseUsername = config.getString("database.username")
        if (databaseUsername != null) {
            hikariConfig.username = databaseUsername
        }
        val databasePassword = config.getString("database.password")
        if (databasePassword != null) {
            hikariConfig.password = databasePassword
        }
        dataSource = HikariDataSource(hikariConfig)
        val oldClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = classLoader
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

        System.setProperty("org.jooq.no-logo", "true")
        System.setProperty("org.jooq.no-tips", "true")

        val dialect = config.getString("database.dialect")?.let(SQLDialect::valueOf)
        val jooqSettings = Settings().withRenderSchema(false)
        val dsl = DSL.using(
            dataSource,
            dialect,
            jooqSettings
        )

        flags = MfFlags(this)

        val gson = GsonBuilder().registerTypeAdapter(MfFactionPermission::class.java, MfFactionPermissionSerializer(flags)).create()
        val playerRepository: MfPlayerRepository = JooqMfPlayerRepository(this, dsl)
        val factionRepository: MfFactionRepository = JooqMfFactionRepository(this, dsl, gson)
        val lawRepository: MfLawRepository = JooqMfLawRepository(dsl)
        val factionRelationshipRepository: MfFactionRelationshipRepository = JooqMfFactionRelationshipRepository(dsl)
        val claimedChunkRepository: MfClaimedChunkRepository = JooqMfClaimedChunkRepository(dsl)
        val lockRepository: MfLockRepository = JooqMfLockRepository(dsl)
        val interactionStatusRepository: MfInteractionStatusRepository = JooqMfInteractionStatusRepository(dsl)
        val gateRepository: MfGateRepository = JooqMfGateRepository(this, dsl)
        val gateCreationContextRepository: MfGateCreationContextRepository = JooqMfGateCreationContextRepository(dsl)

        val playerService = MfPlayerService(playerRepository)
        val factionService = MfFactionService(this, factionRepository)
        val lawService = MfLawService(lawRepository)
        val factionRelationshipService = MfFactionRelationshipService(this, factionRelationshipRepository)
        val claimedChunkService = MfClaimService(this, claimedChunkRepository)
        val lockService = MfLockService(this, lockRepository)
        val interactionService = MfInteractionService(interactionStatusRepository)
        val notificationService = setupNotificationService()
        val gateService = MfGateService(this, gateRepository, gateCreationContextRepository)

        lockService.loadLockedBlocks()
        gateService.loadGates()

        services = Services(
            playerService,
            factionService,
            lawService,
            factionRelationshipService,
            claimedChunkService,
            lockService,
            interactionService,
            notificationService,
            gateService
        )
        setupRpkLockService()

        server.pluginManager.registerEvents(AsyncPlayerPreLoginListener(this), this)
        server.pluginManager.registerEvents(PlayerInteractListener(this), this)
        server.pluginManager.registerEvents(PlayerJoinListener(this), this)
        server.pluginManager.registerEvents(PlayerMoveListener(this), this)
        server.pluginManager.registerEvents(PlayerQuitListener(this), this)

        getCommand("medievalfactions")?.setExecutor(MedievalFactionsCommand(this))
        getCommand("faction")?.setExecutor(MfFactionCommand(this))
        getCommand("lock")?.setExecutor(MfLockCommand(this))
        getCommand("unlock")?.setExecutor(MfUnlockCommand(this))
        getCommand("accessors")?.setExecutor(MfAccessorsCommand(this))
        getCommand("power")?.setExecutor(MfPowerCommand(this))
        getCommand("gate")?.setExecutor(MfGateCommand(this))

        server.scheduler.scheduleSyncRepeatingTask(this, {
            val onlinePlayers = server.onlinePlayers
            val onlineMfPlayerIds = onlinePlayers.map { MfPlayerId(it.uniqueId.toString()) }
            server.scheduler.runTaskAsynchronously(this, Runnable {
                playerService.updatePlayerPower(onlineMfPlayerIds)
            })
        }, (60 - LocalTime.now().minute) * 60 * 20L, 72000L)
        server.scheduler.scheduleSyncRepeatingTask(this, {
            val gates = gateService.gates
            gates.filter(MfGate::shouldOpen).forEach(MfGate::open)
            gates.filter(MfGate::shouldClose).forEach(MfGate::close)
        }, 20L, 20L)

        server.scheduler.scheduleSyncRepeatingTask(this, {
            gateService.getGatesByStatus(CLOSING).forEach(MfGate::continueClosing)
        }, 20L, 5L)
        server.scheduler.scheduleSyncRepeatingTask(this, {
            gateService.getGatesByStatus(OPENING).forEach(MfGate::continueOpening)
        }, 20L, 20L)
    }

    private fun setupNotificationService(): MfNotificationService = when {
        server.pluginManager.getPlugin("Mailboxes") != null -> MailboxesNotificationService(this)
        server.pluginManager.getPlugin("rpk-notification-lib-bukkit") != null -> RpkNotificationService()
        else -> NoOpNotificationService()
    }

    private fun setupRpkLockService() {
        if (server.pluginManager.getPlugin("rpk-lock-lib-bukkit") != null) {
            MfRpkLockService(this)
        }
    }

}
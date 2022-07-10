package dansplugins.factionsystem

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dansplugins.factionsystem.command.MedievalFactionsCommand
import dansplugins.factionsystem.command.faction.MfFactionCommand
import dansplugins.factionsystem.faction.JooqMfFactionRepository
import dansplugins.factionsystem.faction.MfFactionRepository
import dansplugins.factionsystem.faction.MfFactionService
import dansplugins.factionsystem.faction.flag.MfFlags
import dansplugins.factionsystem.lang.Language
import dansplugins.factionsystem.law.JooqMfLawRepository
import dansplugins.factionsystem.law.MfLawRepository
import dansplugins.factionsystem.law.MfLawService
import dansplugins.factionsystem.listener.AsyncPlayerPreLoginListener
import dansplugins.factionsystem.player.JooqMfPlayerRepository
import dansplugins.factionsystem.player.MfPlayerRepository
import dansplugins.factionsystem.player.MfPlayerService
import dansplugins.factionsystem.service.Services
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.flywaydb.core.Flyway
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
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
            .locations("classpath:dansplugins/factionsystem/db/migration")
            .table("mf_schema_history")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .validateOnMigrate(false)
            .load()
        flyway.migrate()
        Thread.currentThread().contextClassLoader = oldClassLoader
        val dialect = config.getString("database.dialect")?.let(SQLDialect::valueOf)
        val jooqSettings = Settings().withRenderSchema(false)
        val dsl = DSL.using(
            dataSource,
            dialect,
            jooqSettings
        )

        flags = MfFlags(this)

        val playerRepository: MfPlayerRepository = JooqMfPlayerRepository(dsl)
        val factionRepository: MfFactionRepository = JooqMfFactionRepository(this, dsl)
        val lawRepository: MfLawRepository = JooqMfLawRepository(dsl)
        val playerService = MfPlayerService(playerRepository)
        val factionService = MfFactionService(factionRepository)
        val lawService = MfLawService(lawRepository)
        services = Services(
            playerService,
            factionService,
            lawService
        )

        server.pluginManager.registerEvents(AsyncPlayerPreLoginListener(playerService), this)

        getCommand("medievalfactions")?.setExecutor(MedievalFactionsCommand(this))
        getCommand("faction")?.setExecutor(MfFactionCommand(this))
    }

}
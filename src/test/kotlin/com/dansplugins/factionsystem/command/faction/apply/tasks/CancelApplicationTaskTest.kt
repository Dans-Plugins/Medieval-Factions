
import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.command.faction.apply.tasks.CancelApplicationTask
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.logging.Level
import java.util.logging.Logger

class CancelApplicationTaskTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var sender: Player
    private lateinit var factionService: MfFactionService
    private lateinit var playerService: MfPlayerService
    private lateinit var language: Language
    private lateinit var logger: Logger

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        sender = mock(Player::class.java)
        factionService = mock(MfFactionService::class.java)
        playerService = mock(MfPlayerService::class.java)
        language = mock(Language::class.java)
        logger = mock(Logger::class.java)
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.factionService).thenReturn(factionService)
        `when`(services.playerService).thenReturn(playerService)
        `when`(plugin.language).thenReturn(language)
        `when`(plugin.logger).thenReturn(logger)
    }

    @Test
    fun testRun_PlayerNotFound() {
        // prepare
        val targetFactionName = "targetFaction"
        `when`(playerService.getPlayer(sender)).thenReturn(null)
        `when`(language["CommandFactionApplyCancelFailedToSaveFaction"]).thenReturn("Failed to save player")

        // execute
        CancelApplicationTask(plugin, sender, targetFactionName).run()

        // verify
        verify(sender).sendMessage("${ChatColor.RED}Failed to save player")
        verify(logger).log(eq(Level.SEVERE), anyString(), any(Throwable::class.java))
    }
}

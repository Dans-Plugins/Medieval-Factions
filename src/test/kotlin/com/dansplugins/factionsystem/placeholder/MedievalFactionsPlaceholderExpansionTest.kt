
import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.placeholder.MedievalFactionsPlaceholderExpansion
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.OfflinePlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

class MedievalFactionsPlaceholderExpansionTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var placeholderExpansion: MedievalFactionsPlaceholderExpansion
    private lateinit var language: Language

    @BeforeEach
    fun setUp() {
        // Mocking MedievalFactions plugin and Language
        plugin = mock(MedievalFactions::class.java)
        language = mock(Language::class.java)

        // Mock the plugin.name to avoid NullPointerException
        `when`(plugin.name).thenReturn("MedievalFactions")

        // Ensure language locale is non-null
        `when`(plugin.language).thenReturn(language)
        `when`(language.locale).thenReturn(Locale.ENGLISH)

        // Mock the services and its methods
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        // Initialize the placeholder expansion
        placeholderExpansion = MedievalFactionsPlaceholderExpansion(plugin)
    }

    @Test
    fun testGetIdentifier() {
        val identifier = placeholderExpansion.identifier
        assertEquals("MedievalFactions", identifier)
    }

    @Test
    fun testFactionName() {
        val player = mock(OfflinePlayer::class.java)
        val factionService = mock(MfFactionService::class.java)
        val playerService = mock(MfPlayerService::class.java)
        val faction = mock(MfFaction::class.java)
        val mfPlayer = mock(MfPlayer::class.java)

        `when`(plugin.services.factionService).thenReturn(factionService)
        `when`(plugin.services.playerService).thenReturn(playerService)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)
        `when`(factionService.getFaction(mfPlayer.id)).thenReturn(faction)
        `when`(faction.name).thenReturn("TestFaction")

        val result = placeholderExpansion.onRequest(player, "faction_name")
        assertEquals("TestFaction", result)
    }
}

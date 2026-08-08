package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.flag.MfFlag
import com.dansplugins.factionsystem.faction.flag.MfFlagValues
import com.dansplugins.factionsystem.faction.flag.MfFlags
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.placeholder.MfPlaceholderResolver
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import java.util.UUID

class MfChatServiceTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var language: Language
    private lateinit var flags: MfFlags
    private lateinit var faction: MfFaction
    private lateinit var mfPlayer: MfPlayer
    private lateinit var sender: OfflinePlayer
    private lateinit var repo: MfChatChannelMessageRepository
    private lateinit var mockedBukkit: MockedStatic<Bukkit>

    private val colorFlag = MfFlag.string("color", FACTION_COLOR)
    private val senderUuid: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockedBukkit = mockStatic(Bukkit::class.java)

        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        language = mock(Language::class.java)
        flags = mock(MfFlags::class.java)
        faction = mock(MfFaction::class.java)
        repo = mock(MfChatChannelMessageRepository::class.java)
        sender = mock(OfflinePlayer::class.java)

        `when`(plugin.config).thenReturn(config)
        `when`(plugin.language).thenReturn(language)
        `when`(plugin.flags).thenReturn(flags)
        `when`(flags.color).thenReturn(colorFlag)
        `when`(language["NoRole"]).thenReturn("No Role")
        `when`(faction.name).thenReturn("TestFaction")
        `when`(faction.flags).thenReturn(MfFlagValues(plugin, mapOf("color" to FACTION_COLOR)))
        `when`(config.getString("chat.faction.format")).thenReturn(CHAT_FORMAT)

        mockedBukkit.`when`<OfflinePlayer> { Bukkit.getOfflinePlayer(senderUuid) }.thenReturn(sender)
        `when`(sender.name).thenReturn("Bilbo")
        `when`(sender.player).thenReturn(null)

        mfPlayer = MfPlayer(MfPlayerId(senderUuid.toString()), chatChannel = MfFactionChatChannel.FACTION)
    }

    @AfterEach
    fun tearDown() {
        mockedBukkit.close()
    }

    @Test
    fun formatMessage_ShouldPassFactionTokensToThePlaceholderResolverAlreadySubstituted() {
        val resolver = RecordingPlaceholderResolver()
        val uut = MfChatService(plugin, repo, resolver)

        uut.formatMessage(mfPlayer, faction, MfFactionChatChannel.FACTION, "hello")

        assertEquals(
            "&7[${ChatColor.of(FACTION_COLOR)}TestFaction&7] [No Role] &f%testplugin_card_name%: hello",
            resolver.lastText
        )
    }

    @Test
    fun formatMessage_ShouldResolvePlaceholdersAgainstTheMessageSender() {
        val resolver = RecordingPlaceholderResolver()
        val uut = MfChatService(plugin, repo, resolver)

        uut.formatMessage(mfPlayer, faction, MfFactionChatChannel.FACTION, "hello")

        assertSame(sender, resolver.lastPlayer)
    }

    @Test
    fun formatMessage_ShouldTranslateColourCodesEmittedByAPlaceholder() {
        val resolver = RecordingPlaceholderResolver { text ->
            text.replace("%testplugin_card_name%", "&bBilbo Baggins")
        }
        val uut = MfChatService(plugin, repo, resolver)

        val result = uut.formatMessage(mfPlayer, faction, MfFactionChatChannel.FACTION, "hello")

        assertTrue(result.contains("${ChatColor.AQUA}Bilbo Baggins"), "Expected a translated colour code in: $result")
        assertFalse(result.contains("&b"), "Expected no untranslated colour codes in: $result")
    }

    @Test
    fun formatMessage_ResolverLeavingTextUnchanged_ShouldStillFormatTheMessage() {
        val uut = MfChatService(plugin, repo, RecordingPlaceholderResolver())

        val result = uut.formatMessage(mfPlayer, faction, MfFactionChatChannel.FACTION, "hello")

        assertEquals(
            ChatColor.translateAlternateColorCodes(
                '&',
                "&7[${ChatColor.of(FACTION_COLOR)}TestFaction&7] [No Role] &f%testplugin_card_name%: hello"
            ),
            result
        )
    }

    private class RecordingPlaceholderResolver(
        private val resolution: (String) -> String = { text -> text }
    ) : MfPlaceholderResolver {

        var lastPlayer: OfflinePlayer? = null
        var lastText: String? = null

        override fun resolve(player: OfflinePlayer, text: String): String {
            lastPlayer = player
            lastText = text
            return resolution(text)
        }
    }

    companion object {
        private const val FACTION_COLOR = "#aabbcc"
        private const val CHAT_FORMAT = "&7[\${factionColor}\${faction}&7] [\${role}] &f%testplugin_card_name%: \${message}"
    }
}

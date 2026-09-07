package com.dansplugins.factionsystem.placeholder

import com.dansplugins.factionsystem.MedievalFactions
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.OfflinePlayer
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

class MfPlaceholderApiResolverTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var server: Server
    private lateinit var pluginManager: PluginManager
    private lateinit var player: OfflinePlayer
    private lateinit var mockedPlaceholderApi: MockedStatic<PlaceholderAPI>
    private lateinit var uut: MfPlaceholderApiResolver

    @BeforeEach
    fun setUp() {
        mockedPlaceholderApi = mockStatic(PlaceholderAPI::class.java)

        plugin = mock(MedievalFactions::class.java)
        server = mock(Server::class.java)
        pluginManager = mock(PluginManager::class.java)
        player = mock(OfflinePlayer::class.java)

        `when`(plugin.server).thenReturn(server)
        `when`(server.pluginManager).thenReturn(pluginManager)

        uut = MfPlaceholderApiResolver(plugin)
    }

    @AfterEach
    fun tearDown() {
        mockedPlaceholderApi.close()
    }

    @Test
    fun resolve_PlaceholderApiNotInstalled_ShouldReturnTextUnchanged() {
        `when`(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null)

        val result = uut.resolve(player, "&fHello %someplugin_some_placeholder%")

        assertEquals("&fHello %someplugin_some_placeholder%", result)
    }

    @Test
    fun resolve_PlaceholderApiNotInstalled_ShouldNotCallPlaceholderApi() {
        `when`(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(null)

        uut.resolve(player, "&fHello %someplugin_some_placeholder%")

        mockedPlaceholderApi.verifyNoInteractions()
    }

    @Test
    fun resolve_PlaceholderApiInstalled_ShouldReturnResolvedText() {
        `when`(pluginManager.getPlugin("PlaceholderAPI")).thenReturn(mock(Plugin::class.java))
        mockedPlaceholderApi.`when`<String> {
            PlaceholderAPI.setPlaceholders(player, "&fHello %someplugin_some_placeholder%")
        }.thenReturn("&fHello Bilbo")

        val result = uut.resolve(player, "&fHello %someplugin_some_placeholder%")

        assertEquals("&fHello Bilbo", result)
    }
}

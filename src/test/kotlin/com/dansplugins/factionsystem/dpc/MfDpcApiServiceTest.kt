package com.dansplugins.factionsystem.dpc

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.faction.MfFactionMember
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.service.Services
import com.google.gson.JsonParser
import org.bukkit.Server
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class MfDpcApiServiceTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var httpClient: HttpClient
    private lateinit var logger: Logger
    private lateinit var uut: MfDpcApiService

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T = ArgumentMatchers.any<T>() as T

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        httpClient = mock(HttpClient::class.java)
        logger = mock(Logger::class.java)

        `when`(plugin.config).thenReturn(config)
        `when`(plugin.logger).thenReturn(logger)

        val server = mock(Server::class.java)
        `when`(plugin.server).thenReturn(server)
        `when`(server.ip).thenReturn("192.168.1.1")
        `when`(server.port).thenReturn(25565)

        uut = MfDpcApiService(plugin, httpClient)
    }

    @Test
    fun testSyncFactions_correctFieldsForNormalFaction() {
        setupEnabledConfig()
        setupFactions(listOf(createMockFaction("TestFaction", "A test faction", 5)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        assertEquals(1, arr.size())
        val obj = arr[0].asJsonObject
        assertEquals("TestFaction", obj.get("name").asString)
        assertEquals("my-server", obj.get("serverId").asString)
        assertEquals(5, obj.get("memberCount").asInt)
        assertEquals("A test faction", obj.get("description").asString)
        assertTrue(obj.has("serverIp"))
        assertTrue(obj.has("discordLink"))
        assertEquals("https://discord.gg/test", obj.get("discordLink").asString)
    }

    @Test
    fun testSyncFactions_serverIpOmittedWhenShareIpFalse() {
        setupEnabledConfig(shareServerIp = false)
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 2)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertFalse(obj.has("serverIp"))
    }

    @Test
    fun testSyncFactions_discordLinkOmittedWhenBlank() {
        setupEnabledConfig(discordLink = "")
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertFalse(obj.has("discordLink"))
    }

    @Test
    fun testSyncFactions_discordLinkOmittedWhenInvalidFormat() {
        setupEnabledConfig(discordLink = "https://example.com/not-discord")
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertFalse(obj.has("discordLink"))
    }

    @Test
    fun testSyncFactions_memberCountIncludedAndNonNegative() {
        setupEnabledConfig()
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 0)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertTrue(obj.has("memberCount"))
        assertTrue(obj.get("memberCount").asInt >= 0)
    }

    @Test
    fun testSyncFactions_fieldsTruncatedWhenOversized() {
        val longName = "A".repeat(100)
        val longDescription = "B".repeat(1000)
        val longDiscordLink = "https://discord.gg/" + "C".repeat(600)
        setupEnabledConfig(discordLink = longDiscordLink, serverId = "D".repeat(100))
        setupFactions(listOf(createMockFaction(longName, longDescription, 3)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertTrue(obj.get("name").asString.length <= MfDpcApiService.MAX_NAME)
        assertTrue(obj.get("serverId").asString.length <= MfDpcApiService.MAX_SERVER_ID)
        assertTrue(obj.get("description").asString.length <= MfDpcApiService.MAX_DESCRIPTION)
        assertTrue(obj.get("discordLink").asString.length <= MfDpcApiService.MAX_DISCORD_LINK)
    }

    @Test
    fun testSyncFactions_skippedWhenServerIdBlank() {
        `when`(config.getBoolean("dpc-api.enabled")).thenReturn(true)
        `when`(config.getString("dpc-api.url")).thenReturn("https://dansplugins.com/api/v1/factions")
        `when`(config.getString("dpc-api.key")).thenReturn("test-key")
        `when`(config.getString("dpc-api.server-id")).thenReturn("")

        val services = mock(Services::class.java)
        val factionService = mock(MfFactionService::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.factionService).thenReturn(factionService)
        `when`(factionService.factions).thenReturn(emptyList())

        uut.syncFactions()

        verify(httpClient, never()).sendAsync(
            anyNonNull<HttpRequest>(),
            anyNonNull<HttpResponse.BodyHandler<String>>()
        )
    }

    @Test
    fun testSyncFactions_skippedWhenNoFactions() {
        // Even with a fully valid config, an empty roster must never be POSTed.
        // The provider treats an empty array as a no-op, but skipping the send
        // here is defense-in-depth: a transient empty read (faction data not yet
        // loaded at startup, or a reload mid-cycle) can never reach the wire.
        setupEnabledConfig()
        setupFactions(emptyList())

        uut.syncFactions()

        verify(httpClient, never()).sendAsync(
            anyNonNull<HttpRequest>(),
            anyNonNull<HttpResponse.BodyHandler<String>>()
        )
    }

    @Test
    fun testSyncFactions_nonDefaultPortIncludedInServerIp() {
        setupEnabledConfig()
        val server = plugin.server
        `when`(server.ip).thenReturn("10.0.0.1")
        `when`(server.port).thenReturn(25570)
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertEquals("10.0.0.1:25570", obj.get("serverIp").asString)
    }

    @Test
    fun testSyncFactions_configuredServerAddressOverridesBinding() {
        setupEnabledConfig()
        `when`(config.getString("dpc-api.server-address")).thenReturn("play.example.com:25565")
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        val obj = arr[0].asJsonObject
        assertEquals("play.example.com:25565", obj.get("serverIp").asString)
    }

    @Test
    fun testSyncFactions_skippedWhenServerIdHasInvalidChars() {
        // Whitespace and other punctuation are intentionally rejected client-side
        // so a malformed server-id can't reach the registry and partition it under
        // a near-duplicate of a real id.
        setupEnabledConfig(serverId = "my server!")
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))

        uut.syncFactions()

        verify(httpClient, never()).sendAsync(
            anyNonNull<HttpRequest>(),
            anyNonNull<HttpResponse.BodyHandler<String>>()
        )
    }

    @Test
    fun testSyncFactions_acceptsServerIdWithAllowedSpecialChars() {
        setupEnabledConfig(serverId = "my.server_1:25565-prod")
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))
        setupHttpResponse(200)

        uut.syncFactions()

        val json = captureRequestBody()
        val arr = JsonParser.parseString(json).asJsonArray
        assertEquals("my.server_1:25565-prod", arr[0].asJsonObject.get("serverId").asString)
    }

    @Test
    fun testSyncFactions_warnsOncePerEnableOnPlainHttpUrl() {
        setupEnabledConfig()
        `when`(config.getString("dpc-api.url")).thenReturn("http://insecure.example.com/api/v1/factions")
        setupFactions(listOf(createMockFaction("TestFaction", "desc", 1)))
        setupHttpResponse(200)

        uut.syncFactions()
        uut.syncFactions()

        // The plain-http warning should appear exactly once across multiple syncs;
        // the sync itself proceeds so operators are not locked out of using a self-
        // hosted dev API on http.
        verify(logger, org.mockito.Mockito.times(1)).warning(
            org.mockito.ArgumentMatchers.contains("plain http://")
        )
    }

    // --- helpers ---

    private fun setupEnabledConfig(
        shareServerIp: Boolean = true,
        discordLink: String = "https://discord.gg/test",
        serverId: String = "my-server"
    ) {
        `when`(config.getBoolean("dpc-api.enabled")).thenReturn(true)
        `when`(config.getString("dpc-api.url")).thenReturn("https://dansplugins.com/api/v1/factions")
        `when`(config.getString("dpc-api.key")).thenReturn("test-api-key")
        `when`(config.getString("dpc-api.server-id")).thenReturn(serverId)
        `when`(config.getBoolean("dpc-api.share-server-ip")).thenReturn(shareServerIp)
        `when`(config.getString("dpc-api.discord-link")).thenReturn(discordLink)
        `when`(config.getString("dpc-api.server-address")).thenReturn("")
    }

    private fun setupFactions(factions: List<MfFaction>) {
        val services = mock(Services::class.java)
        val factionService = mock(MfFactionService::class.java)
        `when`(plugin.services).thenReturn(services)
        `when`(services.factionService).thenReturn(factionService)
        `when`(factionService.factions).thenReturn(factions)
    }

    private fun createMockFaction(name: String, description: String, memberCount: Int): MfFaction {
        val faction = mock(MfFaction::class.java)
        `when`(faction.name).thenReturn(name)
        `when`(faction.description).thenReturn(description)
        val members = (1..memberCount).map { mock(MfFactionMember::class.java) }
        `when`(faction.members).thenReturn(members)
        return faction
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupHttpResponse(statusCode: Int) {
        val response = mock(HttpResponse::class.java) as HttpResponse<String>
        `when`(response.statusCode()).thenReturn(statusCode)
        `when`(response.body()).thenReturn("{}")
        val future = CompletableFuture.completedFuture(response)
        doReturn(future).`when`(httpClient).sendAsync(
            anyNonNull<HttpRequest>(),
            anyNonNull<HttpResponse.BodyHandler<String>>()
        )
    }

    private fun captureRequestBody(): String {
        val captor = ArgumentCaptor.forClass(HttpRequest::class.java)
        verify(httpClient).sendAsync(
            captor.capture(),
            anyNonNull<HttpResponse.BodyHandler<String>>()
        )
        val request = captor.value
        val bodyPublisher = request.bodyPublisher().orElseThrow()
        val future = CompletableFuture<String>()
        bodyPublisher.subscribe(object : Flow.Subscriber<ByteBuffer> {
            private val chunks = mutableListOf<ByteArray>()
            override fun onSubscribe(subscription: Flow.Subscription) {
                subscription.request(Long.MAX_VALUE)
            }
            override fun onNext(item: ByteBuffer) {
                val bytes = ByteArray(item.remaining())
                item.get(bytes)
                chunks.add(bytes)
            }
            override fun onError(throwable: Throwable) {
                future.completeExceptionally(throwable)
            }
            override fun onComplete() {
                val totalSize = chunks.sumOf { it.size }
                val combined = ByteArray(totalSize)
                var offset = 0
                for (chunk in chunks) {
                    chunk.copyInto(combined, offset)
                    offset += chunk.size
                }
                future.complete(String(combined, StandardCharsets.UTF_8))
            }
        })
        return future.get(5, TimeUnit.SECONDS)
    }
}

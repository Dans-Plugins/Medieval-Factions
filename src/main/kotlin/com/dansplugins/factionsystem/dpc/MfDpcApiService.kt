package com.dansplugins.factionsystem.dpc

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.Gson
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.logging.Level

/**
 * Pushes the server's current faction roster to the DPC community API.
 *
 * Threading: Bukkit's plugin data structures (the faction service, members lists,
 * etc.) are NOT thread-safe. [collectSnapshot] must always be called from the
 * main server thread; the snapshot it returns is an immutable view that can then
 * be serialized and shipped off the main thread by [dispatchAsync].
 */
class MfDpcApiService(
    private val plugin: MedievalFactions,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
) {

    private val gson = Gson()

    @Volatile
    private var emptyServerIpWarningLogged = false

    @Volatile
    private var httpInsecureWarningLogged = false

    /**
     * Runs the full sync. Reads Bukkit state on the calling thread (must be the
     * main thread) and dispatches the HTTP send asynchronously via the JDK
     * HttpClient, which uses its own thread pool. Callers that are already on
     * an async thread should use [collectSnapshotOnMainThread] + [dispatchAsync].
     */
    fun syncFactions() {
        val snapshot = collectSnapshot() ?: return
        dispatchAsync(snapshot)
    }

    /**
     * Collects a snapshot of all data needed for an HTTP send. Returns null if
     * the sync should be skipped (disabled, misconfigured, etc.). Must be called
     * from the main thread because it touches Bukkit faction state.
     */
    fun collectSnapshot(): SyncSnapshot? {
        val config = plugin.config

        if (!config.getBoolean("dpc-api.enabled")) return null

        val rawApiUrl = config.getString("dpc-api.url")?.trim()
        if (rawApiUrl.isNullOrBlank()) {
            plugin.logger.warning("DPC API URL is not configured. Set dpc-api.url in config.yml. Skipping faction sync.")
            return null
        }

        val apiUri = try {
            URI(rawApiUrl)
        } catch (e: URISyntaxException) {
            plugin.logger.log(Level.WARNING, "DPC API URL is invalid: '$rawApiUrl'. Skipping faction sync.", e)
            return null
        }

        if (!apiUri.isAbsolute || apiUri.host.isNullOrBlank() || apiUri.scheme !in listOf("http", "https")) {
            plugin.logger.warning("DPC API URL must be an absolute HTTP(S) URL with a host. Current value: '$rawApiUrl'. Skipping faction sync.")
            return null
        }

        if (apiUri.scheme == "http" && !httpInsecureWarningLogged) {
            plugin.logger.warning(
                "DPC API URL uses plain http://; your API key will be sent unencrypted. " +
                    "Set dpc-api.url to an https:// endpoint to protect it."
            )
            httpInsecureWarningLogged = true
        }

        val apiKey = config.getString("dpc-api.key")
        if (apiKey == null) {
            plugin.logger.warning("DPC API key is missing from config.yml (dpc-api.key). Skipping faction sync.")
            return null
        }
        if (apiKey.isEmpty()) {
            plugin.logger.warning("DPC API key is not configured. Skipping faction sync.")
            return null
        }

        val serverId = config.getString("dpc-api.server-id")?.takeIf { it.isNotBlank() }
        if (serverId == null) {
            plugin.logger.warning("DPC server ID is not configured. Set dpc-api.server-id in config.yml. Skipping faction sync.")
            return null
        }
        if (!isValidServerId(serverId)) {
            plugin.logger.warning(
                "DPC server ID '$serverId' contains characters the registry rejects. " +
                    "Allowed: letters, digits, dot, underscore, colon, hyphen. Skipping faction sync."
            )
            return null
        }

        val rawDiscordLink = config.getString("dpc-api.discord-link") ?: ""
        val discordLink: String? = if (rawDiscordLink.isNotEmpty()) {
            if (isValidDiscordLink(rawDiscordLink)) {
                truncate(rawDiscordLink, MAX_DISCORD_LINK)
            } else {
                plugin.logger.warning("Invalid discord link format: '$rawDiscordLink'. Must start with https://discord.gg/ or https://discord.com/. Omitting discordLink from sync payload.")
                null
            }
        } else {
            null
        }

        val shareServerIp = config.getBoolean("dpc-api.share-server-ip")
        val serverIp: String? = if (shareServerIp) {
            val serverAddress = config.getString("dpc-api.server-address")?.takeIf { it.isNotBlank() }
            if (serverAddress != null) {
                truncate(serverAddress, MAX_SERVER_IP)
            } else {
                val ip = plugin.server.ip
                val port = plugin.server.port
                if (ip.isNotEmpty()) {
                    val addr = if (port != 25565) "$ip:$port" else ip
                    truncate(addr, MAX_SERVER_IP)
                } else {
                    if (!emptyServerIpWarningLogged) {
                        plugin.logger.warning("Server IP is empty and dpc-api.server-address is not configured. Set dpc-api.server-address in config.yml to share your server address. Omitting serverIp from sync payload.")
                        emptyServerIpWarningLogged = true
                    }
                    null
                }
            }
        } else {
            null
        }

        // Touch Bukkit-managed faction state here, on the main thread.
        val factionService = plugin.services.factionService
        val payloads = factionService.factions.map { faction ->
            DpcFactionPayload(
                name = faction.name.take(MAX_NAME),
                serverId = serverId.take(MAX_SERVER_ID),
                memberCount = maxOf(0, faction.members.size),
                description = faction.description.take(MAX_DESCRIPTION),
                serverIp = serverIp,
                discordLink = discordLink
            )
        }

        // Never POST an empty roster. The provider already treats an empty array
        // as a no-op, so sending one accomplishes nothing — but skipping it here
        // is defense-in-depth: a transient empty read (e.g. faction data not yet
        // loaded during startup, or a reload mid-cycle) can never reach the wire
        // and depend on the provider's guards to avoid a faction wipe.
        if (payloads.isEmpty()) {
            plugin.logger.fine("No factions to sync to the DPC API; skipping this cycle.")
            return null
        }

        return SyncSnapshot(apiUri, apiKey, payloads)
    }

    /**
     * Sends a previously-collected snapshot. Safe to call from any thread; the
     * JDK HttpClient runs the actual I/O on its own thread pool. The completion
     * callbacks log to the plugin logger, which is thread-safe.
     */
    fun dispatchAsync(snapshot: SyncSnapshot) {
        val body = gson.toJson(snapshot.payloads)
        try {
            val request = HttpRequest.newBuilder()
                .uri(snapshot.uri)
                .header("Content-Type", "application/json")
                .header("X-API-Key", snapshot.apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    val status = response.statusCode()
                    if (status in 200..299) {
                        plugin.logger.info("Successfully synced ${snapshot.payloads.size} faction(s) to DPC API.")
                    } else {
                        // Truncate body for the log so a malicious or oversized response
                        // can't blow up the server log; never log the api key.
                        val truncated = response.body()?.take(MAX_LOGGED_BODY) ?: "<empty>"
                        plugin.logger.warning("DPC API returned status $status: $truncated")
                    }
                }
                .exceptionally { throwable ->
                    plugin.logger.log(Level.WARNING, "Failed to sync factions to DPC API.", throwable)
                    null
                }
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to send faction data to DPC API.", e)
        }
    }

    /** Data carried from the main-thread snapshot phase to the async send phase. */
    data class SyncSnapshot(
        val uri: URI,
        val apiKey: String,
        val payloads: List<DpcFactionPayload>
    )

    companion object {
        const val MAX_NAME = 64
        const val MAX_SERVER_ID = 64
        const val MAX_DESCRIPTION = 512
        const val MAX_SERVER_IP = 253
        const val MAX_DISCORD_LINK = 512

        /** Cap on how much of a non-2xx response body we put into a single log line. */
        private const val MAX_LOGGED_BODY = 512

        // Mirrors the server-side Bean Validation @Pattern on FactionRequest.serverId.
        // Kept as a private regex object so the per-sync validation is cheap.
        private val SERVER_ID_PATTERN = Regex("^[A-Za-z0-9._:-]+$")

        fun isValidServerId(serverId: String): Boolean = SERVER_ID_PATTERN.matches(serverId)
    }

    private fun truncate(value: String?, max: Int): String? = value?.take(max)

    private fun isValidDiscordLink(link: String): Boolean =
        link.startsWith("https://discord.gg/") || link.startsWith("https://discord.com/")
}

package com.dansplugins.factionsystem.dpc

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URI
import java.net.URISyntaxException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.logging.Level

class MfDpcApiService(
    private val plugin: MedievalFactions,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
) {

    private val gson = Gson()

    fun syncFactions() {
        if (!plugin.config.getBoolean("dpc-api.enabled")) return

        val rawApiUrl = plugin.config.getString("dpc-api.url")?.trim()
        if (rawApiUrl.isNullOrBlank()) {
            plugin.logger.warning("DPC API URL is not configured. Set dpc-api.url in config.yml. Skipping faction sync.")
            return
        }

        val apiUri = try {
            URI(rawApiUrl)
        } catch (e: URISyntaxException) {
            plugin.logger.log(Level.WARNING, "DPC API URL is invalid: '$rawApiUrl'. Skipping faction sync.", e)
            return
        }

        if (!apiUri.isAbsolute || apiUri.host.isNullOrBlank() || apiUri.scheme !in listOf("http", "https")) {
            plugin.logger.warning("DPC API URL must be an absolute HTTP(S) URL with a host. Current value: '$rawApiUrl'. Skipping faction sync.")
            return
        }

        val apiUrl = apiUri.toString()
        val apiKey = plugin.config.getString("dpc-api.key") ?: return
        if (apiKey.isEmpty()) {
            plugin.logger.warning("DPC API key is not configured. Skipping faction sync.")
            return
        }

        val factionService = plugin.services.factionService
        val factions = factionService.factions

        val shareServerIp = plugin.config.getBoolean("dpc-api.share-server-ip")
        val rawDiscordLink = plugin.config.getString("dpc-api.discord-link") ?: ""
        val serverId = plugin.config.getString("dpc-api.server-id")?.takeIf { it.isNotBlank() }
        if (serverId == null) {
            plugin.logger.warning("DPC server ID is not configured. Set dpc-api.server-id in config.yml. Skipping faction sync.")
            return
        }

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

        val serverIp: String? = if (shareServerIp) {
            val serverAddress = plugin.config.getString("dpc-api.server-address")?.takeIf { it.isNotBlank() }
            if (serverAddress != null) {
                truncate(serverAddress, MAX_SERVER_IP)
            } else {
                val ip = plugin.server.ip
                val port = plugin.server.port
                if (ip.isNotEmpty()) {
                    val addr = if (port != 25565) "$ip:$port" else ip
                    truncate(addr, MAX_SERVER_IP)
                } else {
                    plugin.logger.warning("Server IP is empty and dpc-api.server-address is not configured. Omitting serverIp from sync payload.")
                    null
                }
            }
        } else {
            null
        }

        val jsonArray = JsonArray()
        for (faction in factions) {
            val obj = JsonObject()
            obj.addProperty("name", truncate(faction.name, MAX_NAME))
            obj.addProperty("serverId", truncate(serverId, MAX_SERVER_ID))
            obj.addProperty("memberCount", maxOf(0, faction.members.size))
            obj.addProperty("description", truncate(faction.description, MAX_DESCRIPTION))
            if (serverIp != null) {
                obj.addProperty("serverIp", serverIp)
            }
            if (discordLink != null) {
                obj.addProperty("discordLink", discordLink)
            }
            jsonArray.add(obj)
        }

        val body = gson.toJson(jsonArray)
        val url = apiUrl.trimEnd('/') + "/api/v1/factions"

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept { response ->
                    if (response.statusCode() in 200..299) {
                        plugin.logger.info("Successfully synced ${factions.size} faction(s) to DPC API.")
                    } else {
                        plugin.logger.warning("DPC API returned status ${response.statusCode()}: ${response.body()}")
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

    companion object {
        const val MAX_NAME = 64
        const val MAX_SERVER_ID = 64
        const val MAX_DESCRIPTION = 512
        const val MAX_SERVER_IP = 253
        const val MAX_DISCORD_LINK = 512
    }

    private fun truncate(value: String?, max: Int): String? = value?.take(max)

    private fun isValidDiscordLink(link: String): Boolean =
        link.startsWith("https://discord.gg/") || link.startsWith("https://discord.com/")
}

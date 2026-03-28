package com.dansplugins.factionsystem.dpc

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.URI
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

        val apiUrl = plugin.config.getString("dpc-api.url") ?: return
        val apiKey = plugin.config.getString("dpc-api.key") ?: return
        if (apiKey.isEmpty()) {
            plugin.logger.warning("DPC API key is not configured. Skipping faction sync.")
            return
        }

        val factionService = plugin.services.factionService
        val factions = factionService.factions

        val shareServerIp = plugin.config.getBoolean("dpc-api.share-server-ip")
        val discordLink = plugin.config.getString("dpc-api.discord-link") ?: ""
        val serverId = plugin.config.getString("dpc-api.server-id")?.takeIf { it.isNotBlank() }
        if (serverId == null) {
            plugin.logger.warning("DPC server ID is not configured. Set dpc-api.server-id in config.yml. Skipping faction sync.")
            return
        }

        val jsonArray = JsonArray()
        for (faction in factions) {
            val obj = JsonObject()
            obj.addProperty("name", faction.name)
            obj.addProperty("serverId", serverId)
            obj.addProperty("memberCount", faction.members.size)
            obj.addProperty("description", faction.description)
            if (shareServerIp) {
                val serverAddress = plugin.config.getString("dpc-api.server-address")?.takeIf { it.isNotBlank() }
                if (serverAddress != null) {
                    obj.addProperty("serverIp", serverAddress)
                } else {
                    val ip = plugin.server.ip
                    val port = plugin.server.port
                    if (ip.isNotEmpty()) {
                        obj.addProperty("serverIp", if (port != 25565) "$ip:$port" else ip)
                    } else {
                        plugin.logger.warning("Server IP is empty and dpc-api.server-address is not configured. Omitting serverIp from sync payload.")
                    }
                }
            }
            if (discordLink.isNotEmpty()) {
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
}

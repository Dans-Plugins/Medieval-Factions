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

class MfDpcApiService(private val plugin: MedievalFactions) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
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
        val serverName = plugin.server.name

        val jsonArray = JsonArray()
        for (faction in factions) {
            val obj = JsonObject()
            obj.addProperty("name", faction.name)
            obj.addProperty("serverId", serverName)
            obj.addProperty("memberCount", faction.members.size)
            obj.addProperty("description", faction.description)
            if (shareServerIp) {
                obj.addProperty("serverIp", plugin.server.ip.ifEmpty { plugin.server.motd })
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

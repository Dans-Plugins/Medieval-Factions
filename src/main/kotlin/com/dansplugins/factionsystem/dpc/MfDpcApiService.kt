package com.dansplugins.factionsystem.dpc

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class MfDpcApiService(private val plugin: MedievalFactions) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val gson = Gson()
    private val playerTokens = ConcurrentHashMap<UUID, String>()

    private fun baseUrl(): String =
        (plugin.config.getString("dpc-api.url") ?: "https://dansplugins.com").trimEnd('/')

    fun getToken(playerUUID: UUID): String? = playerTokens[playerUUID]

    fun clearToken(playerUUID: UUID) {
        playerTokens.remove(playerUUID)
    }

    fun register(
        playerUUID: UUID,
        username: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val body = JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/api/v1/accounts/register"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() in 200..299) {
                    val json = JsonParser.parseString(response.body()).asJsonObject
                    val token = json.get("token")?.asString
                    if (token != null) {
                        playerTokens[playerUUID] = token
                    }
                    val returnedUsername = json.get("username")?.asString ?: username
                    onSuccess(returnedUsername)
                } else {
                    onFailure("HTTP ${response.statusCode()}")
                }
            }
            .exceptionally { throwable ->
                plugin.logger.log(Level.WARNING, "DPC register request failed.", throwable)
                onFailure(throwable.message ?: "Unknown error")
                null
            }
    }

    fun login(
        playerUUID: UUID,
        username: String,
        password: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val body = JsonObject().apply {
            addProperty("username", username)
            addProperty("password", password)
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/api/v1/accounts/login"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() in 200..299) {
                    val json = JsonParser.parseString(response.body()).asJsonObject
                    val token = json.get("token")?.asString
                    if (token != null) {
                        playerTokens[playerUUID] = token
                    }
                    val returnedUsername = json.get("username")?.asString ?: username
                    onSuccess(returnedUsername)
                } else if (response.statusCode() == 401) {
                    onFailure("InvalidCredentials")
                } else {
                    onFailure("HTTP ${response.statusCode()}")
                }
            }
            .exceptionally { throwable ->
                plugin.logger.log(Level.WARNING, "DPC login request failed.", throwable)
                onFailure(throwable.message ?: "Unknown error")
                null
            }
    }

    fun getProfile(
        playerUUID: UUID,
        onSuccess: (JsonObject) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val token = playerTokens[playerUUID]
        if (token == null) {
            onFailure("NotLoggedIn")
            return
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/api/v1/accounts/me"))
            .header("Authorization", "Bearer $token")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() in 200..299) {
                    val json = JsonParser.parseString(response.body()).asJsonObject
                    onSuccess(json)
                } else if (response.statusCode() == 401) {
                    playerTokens.remove(playerUUID)
                    onFailure("SessionExpired")
                } else {
                    onFailure("HTTP ${response.statusCode()}")
                }
            }
            .exceptionally { throwable ->
                plugin.logger.log(Level.WARNING, "DPC profile request failed.", throwable)
                onFailure(throwable.message ?: "Unknown error")
                null
            }
    }

    fun createApiKey(
        playerUUID: UUID,
        serverName: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val token = playerTokens[playerUUID]
        if (token == null) {
            onFailure("NotLoggedIn")
            return
        }
        val body = JsonObject().apply {
            addProperty("serverName", serverName)
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/api/v1/accounts/me/api-keys"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() in 200..299) {
                    val json = JsonParser.parseString(response.body()).asJsonObject
                    val apiKey = json.get("apiKey")?.asString ?: ""
                    onSuccess(apiKey)
                } else if (response.statusCode() == 401) {
                    playerTokens.remove(playerUUID)
                    onFailure("SessionExpired")
                } else {
                    onFailure("HTTP ${response.statusCode()}")
                }
            }
            .exceptionally { throwable ->
                plugin.logger.log(Level.WARNING, "DPC create API key request failed.", throwable)
                onFailure(throwable.message ?: "Unknown error")
                null
            }
    }

    fun deleteApiKey(
        playerUUID: UUID,
        keyId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val token = playerTokens[playerUUID]
        if (token == null) {
            onFailure("NotLoggedIn")
            return
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/api/v1/accounts/me/api-keys/$keyId"))
            .header("Authorization", "Bearer $token")
            .timeout(Duration.ofSeconds(30))
            .DELETE()
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() in 200..299) {
                    onSuccess()
                } else if (response.statusCode() == 401) {
                    playerTokens.remove(playerUUID)
                    onFailure("SessionExpired")
                } else if (response.statusCode() == 404) {
                    onFailure("NotFound")
                } else {
                    onFailure("HTTP ${response.statusCode()}")
                }
            }
            .exceptionally { throwable ->
                plugin.logger.log(Level.WARNING, "DPC delete API key request failed.", throwable)
                onFailure(throwable.message ?: "Unknown error")
                null
            }
    }

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
                obj.addProperty("serverIp", plugin.server.ip.ifEmpty { plugin.server.port.toString() })
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

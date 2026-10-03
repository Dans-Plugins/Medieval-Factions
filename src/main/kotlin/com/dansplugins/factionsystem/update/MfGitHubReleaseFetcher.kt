package com.dansplugins.factionsystem.update

import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Asks GitHub for the latest release with one unauthenticated GET. Nothing
 * about the server or its players is sent: the only header beyond the defaults
 * is a User-Agent naming the plugin and its version, which GitHub requires.
 * `releases/latest` never returns drafts or pre-releases, so the `dev` build is
 * never offered.
 *
 * Never throws. Any failure (offline, timeout, rate limit, unexpected body) is
 * logged at FINE and returns null.
 */
class MfGitHubReleaseFetcher(
    private val userAgent: String,
    private val logger: Logger?,
    private val endpoint: String = LATEST_RELEASE_URL
) : () -> MfLatestRelease? {

    override fun invoke(): MfLatestRelease? {
        return try {
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", userAgent)
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    logger?.fine("Update check: $endpoint answered HTTP $status")
                    return null
                }
                val body = connection.inputStream.use { String(it.readBytes(), StandardCharsets.UTF_8) }
                parse(body)
            } finally {
                connection.disconnect()
            }
        } catch (exception: Exception) {
            logger?.log(Level.FINE, "Update check failed: ${exception.message}", exception)
            null
        }
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/Dans-Plugins/Medieval-Factions/releases/latest"
        const val TIMEOUT_MS = 5_000

        /** Reads `tag_name` and `html_url`; null when either is missing or the release is a draft or pre-release. */
        fun parse(body: String): MfLatestRelease? {
            val json = JsonParser.parseString(body)
            if (!json.isJsonObject) return null
            val obj = json.asJsonObject
            fun string(name: String): String? =
                obj.get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
            fun flag(name: String): Boolean =
                obj.get(name)?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
            if (flag("draft") || flag("prerelease")) return null
            val tag = string("tag_name") ?: return null
            val url = string("html_url") ?: "https://github.com/Dans-Plugins/Medieval-Factions/releases/tag/$tag"
            return MfLatestRelease(tag, url)
        }
    }
}

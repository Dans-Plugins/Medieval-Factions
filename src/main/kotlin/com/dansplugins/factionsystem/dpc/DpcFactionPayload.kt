package com.dansplugins.factionsystem.dpc

/**
 * Data model representing a single faction payload for the DPC API.
 * Null fields are omitted from serialization by Gson's default behavior.
 */
data class DpcFactionPayload(
    val name: String?,
    val serverId: String?,
    val memberCount: Int,
    val description: String?,
    val serverIp: String? = null,
    val discordLink: String? = null
)

package com.dansplugins.factionsystem.legacy

data class MfLegacyLockedBlock(
    val X: String,
    val Y: String,
    val Z: String,
    val owner: String,
    val factionName: String,
    val world: String,
    val accessList: String
)

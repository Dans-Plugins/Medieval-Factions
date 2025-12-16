package com.dansplugins.factionsystem.legacy

data class MfLegacyGate(
    val name: String,
    val factionName: String,
    val open: String,
    val vertical: String,
    val material: String,
    val world: String,
    val coord1: String,
    val coord2: String,
    val triggerCoord: String
)

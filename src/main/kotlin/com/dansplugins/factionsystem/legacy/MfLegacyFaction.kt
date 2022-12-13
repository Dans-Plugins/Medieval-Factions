package com.dansplugins.factionsystem.legacy

data class MfLegacyFaction(
    val members: String,
    val enemyFactions: String,
    val officers: String,
    val allyFactions: String,
    val laws: String,
    val name: String,
    val vassals: String,
    val description: String,
    val owner: String,
    val location: String,
    val liege: String,
    val prefix: String,
    val bonusPower: String,
    val factionGates: String,
    val integerFlagValues: String,
    val booleanFlagValues: String,
    val doubleFlagValues: String,
    val stringFlagValues: String
)

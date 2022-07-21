package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions

class MfFlags(
    plugin: MedievalFactions,
    private val flags: MutableList<MfFlag<Any?>> = mutableListOf(
        MfFlag("alliesCanInteractWithLand", plugin.config.getBoolean("factions.defaults.flags.alliesCanInteractWithLand")),
        MfFlag("vassalageTreeCanInteractWithLand", plugin.config.getBoolean("factions.defaults.flags.vassalageTreeCanInteractWithLand")),
        MfFlag("neutral", plugin.config.getBoolean("factions.defaults.flags.neutral")),
        MfFlag("dynmapTerritoryColor", plugin.config.getString("factions.defaults.flags.dynmapTerritoryColor")),
        MfFlag("territoryAlertColor", plugin.config.getString("factions.defaults.flags.territoryAlertColor")),
        MfFlag("prefixColor", plugin.config.getString("factions.defaults.flags.prefixColor")),
        MfFlag("allowFriendlyFire", plugin.config.getBoolean("factions.defaults.flags.allowFriendlyFire")),
        MfFlag("acceptBonusPower", plugin.config.getBoolean("factions.defaults.flags.acceptBonusPower"))
    )
) : MutableList<MfFlag<Any?>> by flags {

    operator fun <T: Any?> get(name: String) = singleOrNull { it.name == name } as? MfFlag<T>

    val alliesCanInteractWithLand = get<Boolean>("alliesCanInteractWithLand")!!
    val vassalageTreeCanInteractWithLand = get<Boolean>("vassalageTreeCanInteractWithLand")!!
    val isNeutral = get<Boolean>("neutral")!!
    val dynmapTerritoryColor = get<String>("dynmapTerritoryColor")!!
    val territoryAlertColor = get<String>("territoryAlertColor")!!
    val prefixColor = get<String>("prefixColor")!!
    val allowFriendlyFire = get<Boolean>("allowFriendlyFire")!!
    val acceptBonusPower = get<Boolean>("acceptBonusPower")!!

    fun defaults() = MfFlagValues(flags.associateWith(MfFlag<Any?>::defaultValue))

}
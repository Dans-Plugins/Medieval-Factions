package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import java.awt.Color
import kotlin.random.Random

class MfFlags(
    plugin: MedievalFactions,
    private val flags: MutableList<MfFlag<out Any?>> = mutableListOf(
        MfFlag("alliesCanInteractWithLand", plugin.config.getBoolean("factions.defaults.flags.alliesCanInteractWithLand")),
        MfFlag("vassalageTreeCanInteractWithLand", plugin.config.getBoolean("factions.defaults.flags.vassalageTreeCanInteractWithLand")),
        MfFlag("neutral", plugin.config.getBoolean("factions.defaults.flags.neutral")),
        MfFlag("color") {
            val default = plugin.config.getString("factions.defaults.flags.color")
            if (default == "random") {
                val color = Color.getHSBColor(Random.nextFloat(), Random.nextFloat(), 0.8f + (Random.nextFloat() * 0.2f))
                String.format("#%02x%02x%02x", color.red, color.green, color.blue)
            } else {
                default
            }
        },
        MfFlag("allowFriendlyFire", plugin.config.getBoolean("factions.defaults.flags.allowFriendlyFire")),
        MfFlag("acceptBonusPower", plugin.config.getBoolean("factions.defaults.flags.acceptBonusPower"))
    )
) : MutableList<MfFlag<out Any?>> by flags {

    operator fun <T: Any?> get(name: String) = singleOrNull { it.name == name } as? MfFlag<T>

    val alliesCanInteractWithLand = get<Boolean>("alliesCanInteractWithLand")!!
    val vassalageTreeCanInteractWithLand = get<Boolean>("vassalageTreeCanInteractWithLand")!!
    val isNeutral = get<Boolean>("neutral")!!
    val color = get<String>("color")!!
    val allowFriendlyFire = get<Boolean>("allowFriendlyFire")!!
    val acceptBonusPower = get<Boolean>("acceptBonusPower")!!

    fun defaults() = MfFlagValues(flags.associateWith(MfFlag<out Any?>::defaultValue))

}
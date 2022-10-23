package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import java.awt.Color
import kotlin.random.Random

class MfFlags(
    plugin: MedievalFactions,
    private val flags: MutableList<MfFlag<out Any>> = mutableListOf(
        MfFlag.boolean(
            plugin,
            "alliesCanInteractWithLand",
            plugin.config.getBoolean("factions.defaults.flags.alliesCanInteractWithLand")
        ),
        MfFlag.boolean(
            plugin,
            "vassalageTreeCanInteractWithLand",
            plugin.config.getBoolean("factions.defaults.flags.vassalageTreeCanInteractWithLand"),
        ),
        MfFlag.boolean(
            plugin,
            "neutral",
            plugin.config.getBoolean("factions.defaults.flags.neutral")
        ),
        MfFlag.string(
            "color",
            {
                val default = plugin.config.getString("factions.defaults.flags.color") ?: "random"
                if (default == "random") {
                    val color = Color.getHSBColor(Random.nextFloat(), 0.7f + (Random.nextFloat() * 0.3f), 0.3f + (Random.nextFloat() * 0.7f))
                    String.format("#%02x%02x%02x", color.red, color.green, color.blue)
                } else {
                    default
                }
            },
            { value ->
                if (!value.matches(Regex("#[A-Fa-f0-9]{6}"))) {
                    return@string MfFlagValidationFailure(plugin.language["FactionFlagColorValidationFailure"])
                }
                return@string MfFlagValidationSuccess
            }
        ),
        MfFlag.boolean(
            plugin,
            "allowFriendlyFire",
            plugin.config.getBoolean("factions.defaults.flags.allowFriendlyFire")
        ),
        MfFlag.boolean(
            plugin,
            "acceptBonusPower",
            plugin.config.getBoolean("factions.defaults.flags.acceptBonusPower")
        )
    )
) : MutableList<MfFlag<out Any>> by flags {

    operator fun <T: Any> get(name: String) = singleOrNull { it.name.equals(name, ignoreCase = true) } as? MfFlag<T>

    val alliesCanInteractWithLand = get<Boolean>("alliesCanInteractWithLand")!!
    val vassalageTreeCanInteractWithLand = get<Boolean>("vassalageTreeCanInteractWithLand")!!
    val isNeutral = get<Boolean>("neutral")!!
    val color = get<String>("color")!!
    val allowFriendlyFire = get<Boolean>("allowFriendlyFire")!!
    val acceptBonusPower = get<Boolean>("acceptBonusPower")!!

    fun defaults() = MfFlagValues(flags.associateWith(MfFlag<out Any>::defaultValue))

}
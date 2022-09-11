package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.Bukkit
import org.bukkit.configuration.serialization.ConfigurationSerializable

class MfFlagValues(private val values: Map<MfFlag<Any?>, Any?> = mutableMapOf()) : ConfigurationSerializable {
    operator fun <T: Any?> get(flag: MfFlag<T>): T = values[flag as MfFlag<Any?>] as? T ?: flag.defaultValue
    override fun serialize(): Map<String, Any?> {
        return values.mapKeys { (flag, _) -> flag.name }
    }
    companion object {
        @JvmStatic
        fun deserialize(serialized: Map<String, Any?>): MfFlagValues {
            val plugin = Bukkit.getPluginManager().getPlugin("MedievalFactions") as MedievalFactions
            return MfFlagValues(serialized.mapKeys { (flagName, _) ->
                val flag: MfFlag<out Any?>? = plugin.flags[flagName]
                if (flag == null) {
                    plugin.logger.warning("null flag for flag name $flagName")
                }
                plugin.flags[flagName]!!
            })
        }
    }
}
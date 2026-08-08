package com.dansplugins.factionsystem.placeholder

import com.dansplugins.factionsystem.MedievalFactions
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.OfflinePlayer

/**
 * Resolves placeholders using PlaceholderAPI.
 *
 * PlaceholderAPI is only a soft dependency, so its presence is checked before it is used.
 * When it is not installed, text is returned unchanged.
 */
class MfPlaceholderApiResolver(private val plugin: MedievalFactions) : MfPlaceholderResolver {

    override fun resolve(player: OfflinePlayer, text: String): String {
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI") == null) return text
        return PlaceholderAPI.setPlaceholders(player, text)
    }
}

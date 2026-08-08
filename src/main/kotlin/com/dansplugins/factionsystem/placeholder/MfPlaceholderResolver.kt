package com.dansplugins.factionsystem.placeholder

import org.bukkit.OfflinePlayer

/**
 * Resolves placeholders belonging to other plugins inside text produced by Medieval Factions.
 */
interface MfPlaceholderResolver {

    /**
     * Replaces any placeholders in [text] with their values for [player].
     *
     * @param player the player the placeholders are resolved against.
     * @param text the text to resolve placeholders in.
     * @return the text with placeholders replaced, or the text unchanged if no placeholder provider is available.
     */
    fun resolve(player: OfflinePlayer, text: String): String
}

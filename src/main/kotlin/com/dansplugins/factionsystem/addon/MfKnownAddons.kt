package com.dansplugins.factionsystem.addon

/** Whether a known plugin builds on Medieval Factions or merely works well alongside it. */
enum class MfAddonKind {
    /** Declares a hard `depend: [MedievalFactions]` in its plugin.yml. */
    EXPANSION,

    /** Runs without Medieval Factions, but is designed to be used with it. */
    RELATED
}

/**
 * One plugin that Medieval Factions knows about.
 *
 * @property pluginName the exact `name:` from the plugin's plugin.yml, which is what Bukkit looks it up by
 * @property displayName the name shown to operators and reported to bStats
 * @property verifiedVersion for an expansion, the release that passed the dependents boot gate
 * against [MfKnownAddons.VERIFIED_WITH_MF]; null when no such evidence exists
 */
data class MfKnownAddon(
    val pluginName: String,
    val displayName: String,
    val description: String,
    val url: String,
    val kind: MfAddonKind,
    val verifiedVersion: String? = null
)

/**
 * The bundled list of known add-ons, shared by `/mf addons`, the startup line and the
 * `installed_addons` bStats chart. It ships inside the jar; nothing is fetched at runtime.
 */
object MfKnownAddons {

    /**
     * The Medieval Factions release the [MfKnownAddon.verifiedVersion]s were checked against:
     * the dependents gate run recorded in the v7.0.0 release notes, which enabled each
     * expansion's then-current stable release against MF 7.0.0 across two boots.
     */
    const val VERIFIED_WITH_MF = "7.0.0"

    val all: List<MfKnownAddon> = listOf(
        MfKnownAddon(
            pluginName = "Currencies",
            displayName = "Currencies",
            description = "Lets faction owners create and mint local currencies.",
            url = "https://dansplugins.com/resources/currencies",
            kind = MfAddonKind.EXPANSION,
            verifiedVersion = "3.0.0"
        ),
        MfKnownAddon(
            pluginName = "Fiefs",
            displayName = "Fiefs",
            description = "Lets players create fiefs and manage them.",
            url = "https://dansplugins.com/resources/fiefs",
            kind = MfAddonKind.EXPANSION,
            verifiedVersion = "0.12.1"
        ),
        MfKnownAddon(
            pluginName = "Democracy",
            displayName = "Democracy",
            description = "Lets a faction hold elections: candidates, votes and the count.",
            url = "https://dansplugins.com/resources/democracy",
            kind = MfAddonKind.EXPANSION,
            verifiedVersion = "0.3.0"
        ),
        MfKnownAddon(
            // The plugin.yml name is MF_Bluemap; the repository is Bluemap_MedievalFactions.
            pluginName = "MF_Bluemap",
            displayName = "Bluemap_MedievalFactions",
            description = "Renders faction claims on BlueMap's web map.",
            url = "https://dansplugins.com/resources/bluemap-medieval-factions",
            kind = MfAddonKind.EXPANSION,
            // The release notes abbreviate this as v1.0; the tagged jar (MF_Bluemap-1.0.0.jar)
            // reports 1.0.0, which is what an installed copy returns.
            verifiedVersion = "1.0.0"
        ),
        MfKnownAddon(
            pluginName = "MedievalRoleplayEngine",
            displayName = "Medieval Roleplay Engine",
            description = "Facilitates roleplay between players.",
            url = "https://dansplugins.com/resources/medieval-roleplay-engine",
            kind = MfAddonKind.RELATED
        ),
        MfKnownAddon(
            pluginName = "Mailboxes",
            displayName = "Mailboxes",
            description = "Lets players and plugins send persistent messages to players.",
            url = "https://dansplugins.com/resources/mailboxes",
            kind = MfAddonKind.RELATED
        ),
        MfKnownAddon(
            pluginName = "MedievalEconomy",
            displayName = "Medieval Economy",
            description = "A virtual coinpurse and a physical currency item.",
            url = "https://dansplugins.com/resources/medieval-economy",
            kind = MfAddonKind.RELATED
        )
    )
}

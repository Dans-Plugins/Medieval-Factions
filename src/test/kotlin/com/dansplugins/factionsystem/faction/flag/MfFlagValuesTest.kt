package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.lang.Language
import org.bukkit.configuration.file.FileConfiguration
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MfFlagValuesTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration

    @BeforeTest
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        val language = mock(Language::class.java) { invocation -> invocation.arguments.firstOrNull() as? String ?: "" }
        `when`(plugin.language).thenReturn(language)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
    }

    private fun flags(): MfFlags {
        val flags = MfFlags(plugin)
        `when`(plugin.flags).thenReturn(flags)
        return flags
    }

    @Test
    fun aMissingOrNullValueFallsBackToTheDefault() {
        `when`(config.getBoolean("factions.defaults.flags.acceptBonusPower")).thenReturn(true)
        val flags = flags()

        assertEquals(true, MfFlagValues(plugin, emptyMap())[flags.acceptBonusPower])
        assertEquals(true, MfFlagValues(plugin, mapOf("acceptBonusPower" to null))[flags.acceptBonusPower])
        assertEquals(false, MfFlagValues(plugin, mapOf("acceptBonusPower" to false))[flags.acceptBonusPower])
    }

    @Test
    fun addingAFlagValueReturnsANewInstanceAndLeavesTheOriginalUnchanged() {
        val flags = flags()
        val original = MfFlagValues(plugin, mapOf("neutral" to false))

        val updated = original + (flags.isNeutral to true)

        assertEquals(false, original[flags.isNeutral])
        assertEquals(true, updated[flags.isNeutral])
    }

    @Test
    fun addingAMapOfFlagValuesOverridesOnlyTheGivenFlags() {
        val flags = flags()
        val original = MfFlagValues(plugin, mapOf("neutral" to false, "color" to "#111111"))

        val updated = original + mapOf<MfFlag<out Any>, Any?>(flags.color to "#222222")

        assertEquals(false, updated[flags.isNeutral])
        assertEquals("#222222", updated[flags.color])
    }

    @Test
    fun valuesResolveNamesToFlagsAndDropUnknownNames() {
        val flags = flags()
        val flagValues = MfFlagValues(plugin, mapOf("NEUTRAL" to true, "removedFlag" to true))

        assertEquals(mapOf<MfFlag<out Any>, Any?>(flags.isNeutral to true), flagValues.values)
    }

    @Test
    fun aForcedFlagReadsAsItsDefaultWhateverTheFactionStored() {
        `when`(config.getBoolean("factions.defaults.flags.allowFriendlyFire")).thenReturn(true)
        `when`(config.getStringList("factions.forcedFlags")).thenReturn(listOf("ALLOWFRIENDLYFIRE"))
        val flags = flags()
        val stored = MfFlagValues(plugin, mapOf("allowFriendlyFire" to false, "neutral" to true))

        assertEquals(true, stored[flags.allowFriendlyFire])
        assertEquals(true, stored[flags.isNeutral], "flags that are not forced keep their stored value")
        assertEquals(false, stored.valuesByName["allowFriendlyFire"], "the stored value is left untouched")
    }

    @Test
    fun noFlagIsForcedByDefault() {
        val flags = flags()

        assertEquals(false, flags.any(flags::isForced))
        assertEquals(true, MfFlagValues(plugin, mapOf("allowFriendlyFire" to true))[flags.allowFriendlyFire])
    }

    @Test
    fun colorCannotBeForced() {
        `when`(config.getStringList("factions.forcedFlags")).thenReturn(listOf("color"))
        val flags = flags()

        assertEquals(false, flags.isForced(flags.color))
        assertEquals("#111111", MfFlagValues(plugin, mapOf("color" to "#111111"))[flags.color])
    }

    @Test
    fun allowMobGriefingDefaultsFromConfigForFactionsThatNeverSetIt() {
        `when`(config.getBoolean("factions.defaults.flags.allowMobGriefing", true)).thenReturn(true)
        val flags = flags()

        assertEquals(true, MfFlagValues(plugin, mapOf("neutral" to false))[flags.allowMobGriefing])
        assertEquals(false, MfFlagValues(plugin, mapOf("allowMobGriefing" to false))[flags.allowMobGriefing])
    }
}

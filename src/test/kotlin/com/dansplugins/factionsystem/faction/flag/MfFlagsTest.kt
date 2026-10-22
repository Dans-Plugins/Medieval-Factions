package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.lang.Language
import org.bukkit.configuration.file.FileConfiguration
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MfFlagsTest {

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
    fun allBuiltInFlagsAreRegistered() {
        val flags = flags()

        assertEquals(
            listOf(
                "alliesCanInteractWithLand",
                "vassalageTreeCanInteractWithLand",
                "neutral",
                "color",
                "allowFriendlyFire",
                "acceptBonusPower",
                "enableMobProtection",
                "liegeChainCanInteractWithLand",
                "protectVillagerTrade"
            ),
            flags.map { it.name }
        )
    }

    @Test
    fun getIgnoresCaseOfTheFlagName() {
        val flags = flags()

        assertSame(flags.isNeutral, flags.get<Boolean>("NEUTRAL"))
        assertSame(flags.allowFriendlyFire, flags.get<Boolean>("allowfriendlyfire"))
    }

    @Test
    fun getReturnsNullForAnUnknownFlag() {
        val flags = flags()

        assertNull(flags.get<Boolean>("notAFlag"))
    }

    @Test
    fun booleanFlagDefaultsComeFromConfig() {
        `when`(config.getBoolean("factions.defaults.flags.neutral")).thenReturn(true)
        `when`(config.getBoolean("factions.defaults.flags.allowFriendlyFire")).thenReturn(false)

        val flags = flags()

        assertTrue(flags.isNeutral.defaultValue)
        assertFalse(flags.allowFriendlyFire.defaultValue)
    }

    @Test
    fun colorDefaultIsTheConfiguredValue() {
        `when`(config.getString("factions.defaults.flags.color")).thenReturn("#123abc")

        assertEquals("#123abc", flags().color.defaultValue)
    }

    @Test
    fun colorDefaultIsNotValidatedAgainstTheHexFormat() {
        // The configured default is returned as-is; only values set through the flag are validated.
        `when`(config.getString("factions.defaults.flags.color")).thenReturn("blue")

        val color = flags().color

        assertEquals("blue", color.defaultValue)
        assertIs<MfFlagValidationFailure>(color.validate(color.defaultValue))
    }

    @Test
    fun colorDefaultIsARandomHexColorWhenConfiguredAsRandom() {
        `when`(config.getString("factions.defaults.flags.color")).thenReturn("random")

        val color = flags().color

        assertTrue(color.defaultValue.matches(Regex("#[0-9a-f]{6}")), color.defaultValue)
        assertEquals(MfFlagValidationSuccess, color.validate(color.defaultValue))
    }

    @Test
    fun colorDefaultIsARandomHexColorWhenNotConfigured() {
        `when`(config.getString("factions.defaults.flags.color")).thenReturn(null)

        val color = flags().color

        assertTrue(color.defaultValue.matches(Regex("#[0-9a-f]{6}")), color.defaultValue)
    }

    @Test
    fun colorValidationAcceptsSixDigitHexInEitherCase() {
        val color = flags().color

        assertEquals(MfFlagValidationSuccess, color.validate("#a1B2c3"))
        assertEquals(MfFlagValidationSuccess, color.validate("#FFFFFF"))
    }

    @Test
    fun colorValidationRejectsOtherFormats() {
        val color = flags().color

        listOf("red", "a1b2c3", "#abc", "#a1b2c3d4", "#gggggg", "").forEach { value ->
            val result = color.validate(value)
            assertIs<MfFlagValidationFailure>(result, value)
            assertEquals("FactionFlagColorValidationFailure", result.failureMessage)
        }
    }

    @Test
    fun colorCoercionAcceptsAnyString() {
        val result = flags().color.coerce("not a colour")

        assertIs<MfFlagValueCoercionSuccess<*>>(result)
        assertEquals("not a colour", result.value)
    }

    @Test
    fun booleanCoercionAcceptsLowercaseTrueAndFalse() {
        val neutral = flags().isNeutral

        val trueResult = neutral.coerce("true")
        val falseResult = neutral.coerce("false")

        assertIs<MfFlagValueCoercionSuccess<*>>(trueResult)
        assertEquals(true, trueResult.value)
        assertIs<MfFlagValueCoercionSuccess<*>>(falseResult)
        assertEquals(false, falseResult.value)
    }

    @Test
    fun booleanCoercionRejectsAnythingElse() {
        val neutral = flags().isNeutral

        // toBooleanStrictOrNull is case-sensitive, so "TRUE" is refused as well as "yes" and "1".
        listOf("TRUE", "True", "yes", "1", "").forEach { value ->
            val result = neutral.coerce(value)
            assertIs<MfFlagValueCoercionFailure>(result, value)
            assertEquals("FactionFlagBooleanCoercionFailed", result.failureMessage)
        }
    }

    @Test
    fun booleanCoercionRejectsNull() {
        val result = coerceBoolean(plugin)(null)

        assertIs<MfFlagValueCoercionFailure>(result)
    }

    @Test
    fun defaultsHoldEveryFlagsDefaultValue() {
        `when`(config.getBoolean("factions.defaults.flags.enableMobProtection")).thenReturn(true)
        `when`(config.getString("factions.defaults.flags.color")).thenReturn("#000000")
        val flags = flags()

        val defaults = flags.defaults()

        assertEquals(flags.map { it.name }.toSet(), defaults.valuesByName.keys)
        assertEquals(true, defaults[flags.enableMobProtection])
        assertEquals(false, defaults[flags.isNeutral])
        assertEquals("#000000", defaults[flags.color])
    }

    @Test
    fun flagValuesFallBackToTheDefaultForAMissingOrNullValue() {
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
    fun stringFlagWithoutValidationAcceptsAnyValue() {
        val flag = MfFlag.string("motto", "Onwards")

        assertEquals("Onwards", flag.defaultValue)
        assertEquals(MfFlagValidationSuccess, flag.validate(""))
    }

    @Test
    fun withValidationReplacesOnlyTheValidator() {
        val flag = MfFlag.string("motto", "Onwards")
            .withValidation { if (it.isEmpty()) MfFlagValidationFailure("empty") else MfFlagValidationSuccess }

        assertEquals("motto", flag.name)
        assertEquals("Onwards", flag.defaultValue)
        assertEquals(MfFlagValidationFailure("empty"), flag.validate(""))
        assertEquals(MfFlagValidationSuccess, flag.validate("x"))
    }

    @Test
    fun onFailureRunsTheBlockOnlyForAFailure() {
        MfFlagValidationSuccess.onFailure { throw AssertionError("not a failure") }
        MfFlagValueCoercionSuccess(true).onFailure { throw AssertionError("not a failure") }

        val validationError = assertFailsWith<IllegalStateException> {
            MfFlagValidationFailure("bad").onFailure { throw IllegalStateException(it.failureMessage) }
        }
        val coercionError = assertFailsWith<IllegalStateException> {
            MfFlagValueCoercionFailure("worse").onFailure { throw IllegalStateException(it.failureMessage) }
        }
        assertEquals("bad", validationError.message)
        assertEquals("worse", coercionError.message)
    }
}

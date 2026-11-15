package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.lang.Language
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Covers boolean flag values in the server's language. See https://github.com/Dans-Plugins/Medieval-Factions/issues/1708.
 */
class MfFlagValueCoercionTest {

    private val germanWords = mapOf("FlagValueTrue" to "wahr", "FlagValueFalse" to "falsch")

    private fun germanLanguage(): Language =
        mock(Language::class.java) { invocation ->
            val key = invocation.arguments.firstOrNull() as? String ?: ""
            germanWords[key] ?: key
        }

    private fun germanPlugin(): MedievalFactions {
        val plugin = mock(MedievalFactions::class.java)
        val language = germanLanguage()
        `when`(plugin.language).thenReturn(language)
        return plugin
    }

    @Test
    fun `true and false are still accepted exactly as before`() {
        assertEquals(true, parseFlagBoolean("true", "wahr", "falsch"))
        assertEquals(false, parseFlagBoolean("false", "wahr", "falsch"))
    }

    @Test
    fun `the server language's words are accepted, ignoring case and surrounding spaces`() {
        assertEquals(true, parseFlagBoolean("wahr", "wahr", "falsch"))
        assertEquals(true, parseFlagBoolean("WAHR", "wahr", "falsch"))
        assertEquals(false, parseFlagBoolean(" Falsch ", "wahr", "falsch"))
        assertEquals(true, parseFlagBoolean("verdadeiro", "verdadeiro", "falso"))
        assertEquals(false, parseFlagBoolean("faux", "vrai", "faux"))
    }

    @Test
    fun `other words, blanks and null are refused`() {
        assertNull(parseFlagBoolean("yes", "wahr", "falsch"))
        assertNull(parseFlagBoolean("", "wahr", "falsch"))
        assertNull(parseFlagBoolean("   ", "wahr", "falsch"))
        assertNull(parseFlagBoolean(null, "wahr", "falsch"))
    }

    @Test
    fun `coerceBoolean reads the words from the plugin's language`() {
        val coerce = coerceBoolean(germanPlugin())
        // MfFlagValueCoercionSuccess is not a data class, so compare the coerced value, not the wrapper.
        assertEquals(true, assertIs<MfFlagValueCoercionSuccess<*>>(coerce("wahr")).value)
        assertEquals(false, assertIs<MfFlagValueCoercionSuccess<*>>(coerce("falsch")).value)
        assertEquals(true, assertIs<MfFlagValueCoercionSuccess<*>>(coerce("true")).value)
        val failure = coerce("ja")
        assertIs<MfFlagValueCoercionFailure>(failure)
        assertEquals("FactionFlagBooleanCoercionFailed", failure.failureMessage)
    }

    @Test
    fun `boolean values are shown in the server language and other values as stored`() {
        val language = germanLanguage()
        assertEquals("wahr", displayFlagValue(language, true))
        assertEquals("falsch", displayFlagValue(language, false))
        assertEquals("#8e44ad", displayFlagValue(language, "#8e44ad"))
    }
}

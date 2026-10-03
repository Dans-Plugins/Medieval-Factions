package com.dansplugins.factionsystem.lang

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Properties

/**
 * Keys that commands look up must exist in every bundled language file, or players see the raw key
 * ("Missing translation for ...") instead of a message. See
 * https://github.com/Dans-Plugins/Medieval-Factions/issues/2070 and
 * https://github.com/Dans-Plugins/Medieval-Factions/issues/2072.
 */
class BundledLanguageKeysTest {

    private val languageFiles = listOf("lang_en_US", "lang_en_GB", "lang_fr_FR", "lang_de_DE", "lang_pt_BR")

    private fun load(filename: String): Properties {
        val stream = javaClass.classLoader.getResourceAsStream("lang/$filename.properties")
        assertNotNull(stream, "lang/$filename.properties is not bundled")
        return Properties().apply { stream!!.use { load(it) } }
    }

    @Test
    fun `every bundled language file has the keys added for 2070 and 2072`() {
        val requiredKeys = listOf(
            "CommandFactionHelpInvalidPageNumber",
            "CommandFactionHelpFactionClaimCheck",
            "CommandFactionKickKickedNotification",
            "CommandFactionSetNameInvalidFaction"
        )
        languageFiles.forEach { filename ->
            val properties = load(filename)
            requiredKeys.forEach { key ->
                assertTrue(properties.getProperty(key)?.isNotBlank() == true, "$filename is missing $key")
            }
        }
    }

    @Test
    fun `no help line advertises the nonexistent checkclaim command`() {
        languageFiles.forEach { filename ->
            val properties = load(filename)
            assertNull(properties.getProperty("CommandFactionHelpFactionCheckClaim"), "$filename still has the old checkclaim help key")
            properties.stringPropertyNames().filter { it.startsWith("CommandFactionHelp") }.forEach { key ->
                assertFalse(properties.getProperty(key).contains("checkclaim"), "$filename $key mentions checkclaim")
            }
        }
    }

    @Test
    fun `the kicked notification and the unknown-faction hint name their arguments`() {
        languageFiles.forEach { filename ->
            val properties = load(filename)
            assertTrue(properties.getProperty("CommandFactionKickKickedNotification").contains("{0}"), filename)
            val hint = properties.getProperty("CommandFactionSetNameInvalidFaction")
            assertTrue(hint.contains("{0}") && hint.contains("{1}"), filename)
        }
    }
}

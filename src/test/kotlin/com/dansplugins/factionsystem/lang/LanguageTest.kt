package com.dansplugins.factionsystem.lang

import com.dansplugins.factionsystem.MedievalFactions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Path

/**
 * The on-disk language files are copied out of the jar on first run and never updated, so keys added
 * in later releases must fall back to the translations bundled in the jar.
 */
class LanguageTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
    }

    private fun writeOnDiskFile(filename: String, content: String) {
        val languageFolder = File(tempDir.toFile(), "lang")
        languageFolder.mkdirs()
        File(languageFolder, filename).writeText(content, Charsets.ISO_8859_1)
    }

    @Test
    fun `falls back to the bundled translation for a key missing from the on-disk file`() {
        writeOnDiskFile("lang_en_US.properties", "CommandFactionCreateUsage=Usage: /faction create [name]\n")

        val language = Language(plugin, "en-US")

        assertEquals(
            "[Medieval Factions] Want to share your faction data with the DPC community? Get an API key at " +
                "https://dansplugins.com, set dpc-api.key and dpc-api.server-id in config.yml, then run " +
                "/mf dpc optin. Other options: /mf dpc shareip, /mf dpc discord, /mf dpc reminder off to hide " +
                "this message.",
            language["DpcLoginReminder"]
        )
    }

    @Test
    fun `prefers the on-disk translation over the bundled one`() {
        writeOnDiskFile("lang_en_US.properties", "CommandFactionCreateUsage=Operator-edited usage\n")

        val language = Language(plugin, "en-US")

        assertEquals("Operator-edited usage", language["CommandFactionCreateUsage"])
    }

    @Test
    fun `reports a key missing from both the on-disk file and the bundle`() {
        writeOnDiskFile("lang_en_US.properties", "CommandFactionCreateUsage=Usage: /faction create [name]\n")

        val language = Language(plugin, "en-US")

        assertEquals("Missing translation for en-US: NoSuchKey", language["NoSuchKey"])
    }
}

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
import java.util.Properties

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

        val bundled = Properties()
        File("src/main/resources/lang/lang_en_US.properties").inputStream().use { bundled.load(it) }
        assertEquals(bundled.getProperty("DpcLoginReminder"), language["DpcLoginReminder"])
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

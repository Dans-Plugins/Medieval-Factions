package com.dansplugins.factionsystem.lang

import com.dansplugins.factionsystem.MedievalFactions
import org.junit.jupiter.api.Assertions.assertArrayEquals
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
    fun `packaged language files are byte-identical to their sources`() {
        // processResources output is what the jar ships; a resource filter once re-encoded the ISO-8859-1 files
        listOf("lang_en_US", "lang_en_GB", "lang_fr_FR", "lang_de_DE", "lang_pt_BR").forEach { name ->
            val packaged = javaClass.classLoader.getResourceAsStream("lang/$name.properties")!!.use { it.readBytes() }
            val source = File("src/main/resources/lang/$name.properties").readBytes()
            assertArrayEquals(source, packaged, "$name.properties was altered while packaging")
        }
    }

    @Test
    fun `the bundled German translation keeps its umlauts`() {
        writeOnDiskFile("lang_de_DE.properties", "CommandFactionCreateUsage=Verwendung\n")

        val language = Language(plugin, "de-DE")

        assertEquals("Nächste Seite >", language["NextPage"])
    }

    @Test
    fun `an on-disk value damaged by the old packaging bug gives way to the bundled one`() {
        val languageFolder = File(tempDir.toFile(), "lang")
        languageFolder.mkdirs()
        // What earlier builds copied to servers: UTF-8 bytes with U+FFFD where the umlaut was
        File(languageFolder, "lang_de_DE.properties").writeText("NextPage=N�chste Seite >\n", Charsets.UTF_8)

        val language = Language(plugin, "de-DE")

        assertEquals("Nächste Seite >", language["NextPage"])
    }

    @Test
    fun `an intact operator-edited on-disk value still wins`() {
        writeOnDiskFile("lang_de_DE.properties", "NextPage=Weiter ä >\n")

        val language = Language(plugin, "de-DE")

        assertEquals("Weiter ä >", language["NextPage"])
    }

    @Test
    fun `reports a key missing from both the on-disk file and the bundle`() {
        writeOnDiskFile("lang_en_US.properties", "CommandFactionCreateUsage=Usage: /faction create [name]\n")

        val language = Language(plugin, "en-US")

        assertEquals("Missing translation for en-US: NoSuchKey", language["NoSuchKey"])
    }
}

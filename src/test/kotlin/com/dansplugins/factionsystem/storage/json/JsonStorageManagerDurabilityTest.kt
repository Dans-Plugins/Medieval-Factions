package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

/**
 * Durability properties of [JsonStorageManager.writeJsonFile].
 *
 * Writes go to a sibling temporary file and are swapped into place, because opening the destination
 * directly truncates it before any new content is written — a crash at that moment would destroy every
 * entity of that type.
 */
class JsonStorageManagerDurabilityTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager

    data class Payload(val values: List<String>)

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.logger).thenReturn(Logger.getLogger("TestLogger"))
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        `when`(plugin.getResource("schemas/players.json"))
            .thenAnswer { File("src/main/resources/schemas/players.json").inputStream() }
        storageManager = JsonStorageManager(plugin, tempDir.toString())
    }

    @Test
    fun `leaves no temporary file behind after a write`() {
        storageManager.writeJsonFile("payload.json", Payload(listOf("a", "b")))

        val leftovers = tempDir.toFile().listFiles()?.filter { it.name.endsWith(".tmp") } ?: emptyList()
        assertTrue(leftovers.isEmpty(), "temporary files should be swapped into place, found: $leftovers")
        assertTrue(File(tempDir.toFile(), "payload.json").exists())
    }

    @Test
    fun `temporary files never appear in listJsonFiles`() {
        storageManager.writeJsonFile("payload.json", Payload(listOf("a")))
        // Simulate a temp file abandoned by a previous crash.
        File(tempDir.toFile(), "payload.json.tmp").writeText("{\"values\":[")

        assertEquals(listOf("payload.json"), storageManager.listJsonFiles())
    }

    @Test
    fun `overwriting with shorter content leaves no trailing bytes`() {
        storageManager.writeJsonFile("payload.json", Payload(List(200) { "entry-$it" }))
        val longLength = File(tempDir.toFile(), "payload.json").length()

        storageManager.writeJsonFile("payload.json", Payload(listOf("x")))
        val file = File(tempDir.toFile(), "payload.json")

        assertTrue(file.length() < longLength, "the replacement should be shorter than what it replaced")
        val reread = storageManager.readJsonFile("payload.json", Payload::class.java)
        assertEquals(listOf("x"), reread?.values, "a truncated or concatenated file would fail to parse cleanly")
    }

    @Test
    fun `a schema violation leaves the previously stored file untouched`() {
        val schema = storageManager.loadSchemaFromResource("schemas/players.json")
        val valid = mapOf(
            "players" to listOf(
                mapOf(
                    "id" to "11111111-1111-1111-1111-111111111111",
                    "version" to 1,
                    "power" to 1.0,
                    "powerAtLogout" to 1.0,
                    "isBypassEnabled" to false
                )
            )
        )
        storageManager.writeJsonFile("players.json", valid, schema)
        val before = File(tempDir.toFile(), "players.json").readText()

        // "players" must be an array of player objects; a bare string violates the schema.
        assertThrows<Exception> {
            storageManager.writeJsonFile("players.json", mapOf("players" to "not-an-array"), schema)
        }

        assertEquals(before, File(tempDir.toFile(), "players.json").readText(), "a rejected write must not damage the stored file")
    }
}

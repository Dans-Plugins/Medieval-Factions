package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

class JsonStorageManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager

    data class TestData(val name: String, val value: Int)

    @BeforeEach
    fun setup() {
        plugin = mockk<MedievalFactions>(relaxed = true)
        every { plugin.logger } returns Logger.getLogger("TestLogger")
        storageManager = JsonStorageManager(plugin, tempDir.toString())
    }

    @AfterEach
    fun cleanup() {
        // Clean up temp files
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `test write and read JSON file`() {
        val fileName = "test.json"
        val testData = TestData("test", 42)

        storageManager.writeJsonFile(fileName, testData)
        val result = storageManager.readJsonFile(fileName, TestData::class.java)

        assertNotNull(result)
        assertEquals("test", result?.name)
        assertEquals(42, result?.value)
    }

    @Test
    fun `test read non-existent file returns null`() {
        val result = storageManager.readJsonFile("nonexistent.json", TestData::class.java)
        assertNull(result)
    }

    @Test
    fun `test fileExists returns correct result`() {
        val fileName = "exists.json"
        assertFalse(storageManager.fileExists(fileName))

        storageManager.writeJsonFile(fileName, TestData("test", 1))
        assertTrue(storageManager.fileExists(fileName))
    }

    @Test
    fun `test delete JSON file`() {
        val fileName = "delete.json"
        storageManager.writeJsonFile(fileName, TestData("test", 1))
        assertTrue(storageManager.fileExists(fileName))

        val deleted = storageManager.deleteJsonFile(fileName)
        assertTrue(deleted)
        assertFalse(storageManager.fileExists(fileName))
    }

    @Test
    fun `test delete non-existent file returns false`() {
        val deleted = storageManager.deleteJsonFile("nonexistent.json")
        assertFalse(deleted)
    }

    @Test
    fun `test backup JSON file`() {
        val fileName = "backup.json"
        storageManager.writeJsonFile(fileName, TestData("test", 1))

        val backed = storageManager.backupJsonFile(fileName)
        assertTrue(backed)

        val backupDir = File(storageManager.getStorageDirectory(), "backups")
        assertTrue(backupDir.exists())
        assertTrue(backupDir.listFiles()?.any { it.name.startsWith("backup.json.") } ?: false)
    }

    @Test
    fun `test backup non-existent file returns false`() {
        val backed = storageManager.backupJsonFile("nonexistent.json")
        assertFalse(backed)
    }

    @Test
    fun `test getStorageDirectory returns correct directory`() {
        val dir = storageManager.getStorageDirectory()
        assertEquals(tempDir.toFile().absolutePath, dir.absolutePath)
    }

    @Test
    fun `test readJsonFileAsString returns content`() {
        val fileName = "string.json"
        storageManager.writeJsonFile(fileName, TestData("test", 1))

        val content = storageManager.readJsonFileAsString(fileName)
        assertNotNull(content)
        assertTrue(content!!.contains("test"))
        assertTrue(content.contains("1"))
    }

    @Test
    fun `test readJsonFileAsString for non-existent file returns null`() {
        val content = storageManager.readJsonFileAsString("nonexistent.json")
        assertNull(content)
    }

    @Test
    fun `test listJsonFiles returns JSON files`() {
        storageManager.writeJsonFile("file1.json", TestData("test1", 1))
        storageManager.writeJsonFile("file2.json", TestData("test2", 2))

        // Create a non-JSON file
        File(storageManager.getStorageDirectory(), "other.txt").writeText("test")

        val files = storageManager.listJsonFiles()
        assertEquals(2, files.size)
        assertTrue(files.contains("file1.json"))
        assertTrue(files.contains("file2.json"))
    }

    @Test
    fun `test concurrent writes to same file`() {
        val fileName = "concurrent.json"
        val threads = (1..10).map { index ->
            Thread {
                storageManager.writeJsonFile(fileName, TestData("test$index", index))
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // File should exist and contain valid data
        val result = storageManager.readJsonFile(fileName, TestData::class.java)
        assertNotNull(result)
    }
}

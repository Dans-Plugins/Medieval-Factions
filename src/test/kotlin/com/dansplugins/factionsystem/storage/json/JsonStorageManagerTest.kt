package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonStorageManagerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var storageManager: JsonStorageManager
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setup() {
        plugin = mock(MedievalFactions::class.java)
        `when`(plugin.dataFolder).thenReturn(tempDir.toFile())
        storageManager = JsonStorageManager(plugin, tempDir.toString())
    }

    @AfterEach
    fun cleanup() {
        // Clean up any test files
        tempDir.toFile().listFiles()?.forEach { it.deleteRecursively() }
    }

    @Test
    fun testReadJsonArray_EmptyFile() {
        // prepare
        val fileName = "test_empty.json"
        val file = File(tempDir.toFile(), fileName)
        file.writeText("[]")

        // execute
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertNotNull(result)
        assertEquals(0, result.size())
    }

    @Test
    fun testReadJsonArray_NonExistentFile() {
        // prepare
        val fileName = "nonexistent.json"

        // execute
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertNotNull(result)
        assertEquals(0, result.size())
    }

    @Test
    fun testReadJsonArray_ValidData() {
        // prepare
        val fileName = "test_data.json"
        val file = File(tempDir.toFile(), fileName)
        val testArray = JsonArray()
        val obj1 = JsonObject()
        obj1.addProperty("id", "test-1")
        obj1.addProperty("name", "Test Item 1")
        testArray.add(obj1)
        val obj2 = JsonObject()
        obj2.addProperty("id", "test-2")
        obj2.addProperty("name", "Test Item 2")
        testArray.add(obj2)
        file.writeText(testArray.toString())

        // execute
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertNotNull(result)
        assertEquals(2, result.size())
        assertEquals("test-1", result[0].asJsonObject.get("id").asString)
        assertEquals("Test Item 2", result[1].asJsonObject.get("name").asString)
    }

    @Test
    fun testWriteJsonArray_NewFile() {
        // prepare
        val fileName = "test_write.json"
        val testArray = JsonArray()
        val obj = JsonObject()
        obj.addProperty("id", "write-test")
        obj.addProperty("value", 42)
        testArray.add(obj)

        // execute
        storageManager.writeJsonArray(fileName, testArray)

        // verify
        val file = File(tempDir.toFile(), fileName)
        assertTrue(file.exists())
        val content = file.readText()
        assertTrue(content.contains("write-test"))
        assertTrue(content.contains("42"))
    }

    @Test
    fun testWriteJsonArray_OverwriteExisting() {
        // prepare
        val fileName = "test_overwrite.json"
        val file = File(tempDir.toFile(), fileName)
        file.writeText("[{\"old\":\"data\"}]")
        
        val newArray = JsonArray()
        val obj = JsonObject()
        obj.addProperty("new", "data")
        newArray.add(obj)

        // execute
        storageManager.writeJsonArray(fileName, newArray)

        // verify
        val content = file.readText()
        assertTrue(content.contains("new"))
        assertFalse(content.contains("old"))
    }

    @Test
    fun testThreadSafety_ConcurrentReads() {
        // prepare
        val fileName = "test_concurrent.json"
        val file = File(tempDir.toFile(), fileName)
        val testArray = JsonArray()
        for (i in 1..100) {
            val obj = JsonObject()
            obj.addProperty("id", "item-$i")
            testArray.add(obj)
        }
        file.writeText(testArray.toString())

        // execute - simulate concurrent reads
        val threads = List(10) {
            Thread {
                repeat(10) {
                    val result = storageManager.readJsonArray(fileName)
                    assertEquals(100, result.size())
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // verify - no exceptions thrown and data is consistent
        val finalResult = storageManager.readJsonArray(fileName)
        assertEquals(100, finalResult.size())
    }

    @Test
    fun testOptimisticLocking_VersionIncrement() {
        // prepare
        val fileName = "test_versioning.json"
        val testArray = JsonArray()
        val obj = JsonObject()
        obj.addProperty("id", "test-1")
        obj.addProperty("version", 1)
        testArray.add(obj)

        // execute - write twice
        storageManager.writeJsonArray(fileName, testArray)
        val readResult = storageManager.readJsonArray(fileName)
        readResult[0].asJsonObject.addProperty("version", 2)
        storageManager.writeJsonArray(fileName, readResult)

        // verify
        val finalResult = storageManager.readJsonArray(fileName)
        assertEquals(2, finalResult[0].asJsonObject.get("version").asInt)
    }

    @Test
    fun testSpecialCharacters_InData() {
        // prepare
        val fileName = "test_special_chars.json"
        val testArray = JsonArray()
        val obj = JsonObject()
        obj.addProperty("text", "Special chars: @#$% 你好 🎉 \"quotes\" 'apostrophes'")
        testArray.add(obj)

        // execute
        storageManager.writeJsonArray(fileName, testArray)
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertEquals("Special chars: @#$% 你好 🎉 \"quotes\" 'apostrophes'", 
                     result[0].asJsonObject.get("text").asString)
    }

    @Test
    fun testLargeDataSet() {
        // prepare
        val fileName = "test_large_dataset.json"
        val testArray = JsonArray()
        for (i in 1..1000) {
            val obj = JsonObject()
            obj.addProperty("id", "item-$i")
            obj.addProperty("data", "Some data for item $i with a longer string to simulate real data")
            testArray.add(obj)
        }

        // execute
        storageManager.writeJsonArray(fileName, testArray)
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertEquals(1000, result.size())
        assertEquals("item-1", result[0].asJsonObject.get("id").asString)
        assertEquals("item-1000", result[999].asJsonObject.get("id").asString)
    }

    @Test
    fun testEmptyJsonArray_WriteAndRead() {
        // prepare
        val fileName = "test_empty_array.json"
        val testArray = JsonArray()

        // execute
        storageManager.writeJsonArray(fileName, testArray)
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertNotNull(result)
        assertEquals(0, result.size())
    }

    @Test
    fun testNestedJsonObjects() {
        // prepare
        val fileName = "test_nested.json"
        val testArray = JsonArray()
        val obj = JsonObject()
        obj.addProperty("id", "parent-1")
        val nested = JsonObject()
        nested.addProperty("childId", "child-1")
        nested.addProperty("childValue", "nested value")
        obj.add("nested", nested)
        testArray.add(obj)

        // execute
        storageManager.writeJsonArray(fileName, testArray)
        val result = storageManager.readJsonArray(fileName)

        // verify
        assertEquals(1, result.size())
        val parentObj = result[0].asJsonObject
        assertEquals("parent-1", parentObj.get("id").asString)
        val nestedObj = parentObj.getAsJsonObject("nested")
        assertEquals("child-1", nestedObj.get("childId").asString)
        assertEquals("nested value", nestedObj.get("childValue").asString)
    }
}

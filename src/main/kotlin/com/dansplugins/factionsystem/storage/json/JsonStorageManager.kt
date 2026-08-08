package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.FileWriter
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Manages JSON file storage with schema validation and thread-safe operations
 */
class JsonStorageManager(
    private val plugin: MedievalFactions,
    private val storagePath: String
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
        .create()

    private val storageDir: File = File(storagePath).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    // Thread-safe file operations using locks
    private val fileLocks = mutableMapOf<String, ReentrantReadWriteLock>()

    private fun getLock(fileName: String): ReentrantReadWriteLock {
        return synchronized(fileLocks) {
            fileLocks.getOrPut(fileName) { ReentrantReadWriteLock() }
        }
    }

    /**
     * Runs [block] while holding this file's write lock.
     *
     * Repositories mutate a file by loading it, changing the loaded copy and writing it back. Those three
     * steps each take the lock individually, so without an enclosing lock two concurrent callers can both
     * load the same snapshot and the second write silently discards the first one's change. The version
     * check in each repository cannot catch that, because it compares against the same stale snapshot.
     * Wrapping the whole read-modify-write cycle in this method makes it atomic with respect to other
     * callers. The locks are reentrant, so the reads and writes inside [block] may take them again.
     */
    fun <R> withFileLock(fileName: String, block: () -> R): R = getLock(fileName).write(block)

    /**
     * Reads and validates a JSON file
     */
    fun <T> readJsonFile(fileName: String, clazz: Class<T>, schema: Schema? = null): T? {
        val lock = getLock(fileName)
        return lock.read {
            val file = File(storageDir, fileName)
            if (!file.exists()) {
                return@read null
            }

            try {
                val jsonContent = file.readText()

                // Validate against schema if provided
                if (schema != null) {
                    validateJson(jsonContent, schema)
                }

                return@read gson.fromJson(jsonContent, clazz)
            } catch (e: ValidationException) {
                plugin.logger.severe("JSON validation failed for $fileName: ${e.message}")
                plugin.logger.severe("Schema location: ${e.schemaLocation}")
                plugin.logger.severe("Pointer to violation: ${e.pointerToViolation}")
                e.causingExceptions.forEach { cause ->
                    plugin.logger.severe("  - ${cause.message}")
                }
                throw e
            } catch (e: Exception) {
                plugin.logger.severe("Failed to read JSON file $fileName: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * Reads a JSON file as a raw string
     */
    fun readJsonFileAsString(fileName: String): String? {
        val lock = getLock(fileName)
        return lock.read {
            val file = File(storageDir, fileName)
            if (!file.exists()) {
                return@read null
            }
            return@read file.readText()
        }
    }

    /**
     * Writes and validates a JSON file
     */
    fun <T> writeJsonFile(fileName: String, data: T, schema: Schema? = null) {
        val lock = getLock(fileName)
        lock.write {
            try {
                val jsonContent = gson.toJson(data)

                // Validate against schema if provided
                if (schema != null) {
                    validateJson(jsonContent, schema)
                }

                // Write to a sibling temporary file and swap it into place, so that a crash, a kill or a
                // full disk partway through cannot leave a truncated file behind. Opening the real file
                // for writing truncates it immediately, which would destroy every entity of this type.
                val file = File(storageDir, fileName)
                file.parentFile?.mkdirs()
                val tempFile = File(file.parentFile, "$fileName.tmp")
                FileWriter(tempFile).use { writer ->
                    writer.write(jsonContent)
                }
                try {
                    Files.move(
                        tempFile.toPath(),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                    )
                } catch (e: AtomicMoveNotSupportedException) {
                    // Some filesystems cannot promise atomicity; a plain replace is still better than
                    // having truncated the destination up front.
                    Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: ValidationException) {
                plugin.logger.severe("JSON validation failed for $fileName: ${e.message}")
                plugin.logger.severe("Schema location: ${e.schemaLocation}")
                plugin.logger.severe("Pointer to violation: ${e.pointerToViolation}")
                e.causingExceptions.forEach { cause ->
                    plugin.logger.severe("  - ${cause.message}")
                }
                throw e
            } catch (e: Exception) {
                plugin.logger.severe("Failed to write JSON file $fileName: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * Validates JSON content against a schema
     */
    private fun validateJson(jsonContent: String, schema: Schema) {
        val jsonObject = JSONObject(jsonContent)
        schema.validate(jsonObject)
    }

    /**
     * Deletes a JSON file
     */
    fun deleteJsonFile(fileName: String): Boolean {
        val lock = getLock(fileName)
        return lock.write {
            val file = File(storageDir, fileName)
            if (file.exists()) {
                return@write file.delete()
            }
            return@write false
        }
    }

    /**
     * Lists all JSON files in a directory
     */
    fun listJsonFiles(directory: String = ""): List<String> {
        val dir = if (directory.isEmpty()) storageDir else File(storageDir, directory)
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }
        return dir.listFiles()?.filter { it.extension == "json" }?.map { it.name } ?: emptyList()
    }

    /**
     * Checks if a JSON file exists
     */
    fun fileExists(fileName: String): Boolean {
        val file = File(storageDir, fileName)
        return file.exists()
    }

    /**
     * Creates a backup of a JSON file
     */
    fun backupJsonFile(fileName: String): Boolean {
        val lock = getLock(fileName)
        return lock.read {
            val file = File(storageDir, fileName)
            if (!file.exists()) {
                return@read false
            }

            val backupDir = File(storageDir, "backups")
            backupDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "$fileName.$timestamp.backup")

            return@read try {
                file.copyTo(backupFile, overwrite = false)
                true
            } catch (e: Exception) {
                plugin.logger.severe("Failed to backup JSON file $fileName: ${e.message}")
                false
            }
        }
    }

    /**
     * Gets the storage directory
     */
    fun getStorageDirectory(): File = storageDir

    /**
     * Loads a schema from resources
     */
    fun loadSchemaFromResource(resourcePath: String): Schema {
        val schemaStream = plugin.getResource(resourcePath)
            ?: throw IllegalArgumentException("Schema resource not found: $resourcePath")

        val schemaJson = JSONObject(JSONTokener(schemaStream))
        return SchemaLoader.load(schemaJson)
    }
}

/**
 * Custom TypeAdapter for java.time.Instant to handle JSON serialization/deserialization
 */
class InstantTypeAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(`in`: JsonReader): Instant? {
        val value = `in`.nextString()
        return if (value == null || value == "null") null else Instant.parse(value)
    }
}

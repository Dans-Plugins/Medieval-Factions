package com.dansplugins.factionsystem.storage.json

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.failure.UnreadableJsonFileException
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
import java.util.concurrent.ConcurrentHashMap
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

    /**
     * Files whose stored contents were present but could not be parsed.
     *
     * Every repository mutates a file by loading it, changing the loaded copy and writing the whole
     * thing back, so a load that fails and is treated as "no data" is one write away from replacing
     * the stored data with nothing. A file listed here is therefore refused by [writeJsonFile] until
     * it parses again — see [loadJsonData].
     */
    private val unreadableFiles: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private fun getLock(fileName: String): ReentrantReadWriteLock {
        return synchronized(fileLocks) {
            fileLocks.getOrPut(fileName) { ReentrantReadWriteLock() }
        }
    }

    /**
     * Loads the whole contents of [fileName] as [clazz], or [empty] when there is nothing stored yet.
     *
     * This is the single load path every repository uses, so that a file which cannot be read is
     * handled identically for every entity type rather than in twelve hand-copied ways.
     *
     * A file that is absent, or that holds nothing at all, is not an error: that is what a fresh
     * install looks like, and [empty] is returned. A file that holds something which does not parse
     * is a different matter — the stored data is still there, and it is only unreadable to us. In
     * that case the contents are copied aside, the failure is logged, [fileName] is marked unreadable
     * so that [writeJsonFile] refuses to replace it, and [empty] is returned so that the rest of the
     * plugin keeps running without that entity type rather than failing to start.
     *
     * The mark is cleared as soon as the file parses again, so repairing or removing it and letting
     * the plugin read it once is all that is needed to restore writes.
     *
     * @param fileName the file to load, relative to the storage directory
     * @param description what the file holds, for the log message (for example `laws`)
     * @param gson the parser to use, which varies by repository
     * @param clazz the type the whole file deserializes to
     * @param empty produces the value meaning "nothing stored"
     */
    fun <T : Any> loadJsonData(
        fileName: String,
        description: String,
        gson: Gson,
        clazz: Class<T>,
        empty: () -> T
    ): T = getLock(fileName).read {
        val json = readJsonFileAsString(fileName)
        if (json == null || json.isBlank()) {
            unreadableFiles.remove(fileName)
            return@read empty()
        }
        val parsed = try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            markUnreadable(fileName, description, json, e.message)
            return@read empty()
        }
        if (parsed == null) {
            markUnreadable(fileName, description, json, "the file parsed to nothing")
            return@read empty()
        }
        unreadableFiles.remove(fileName)
        return@read parsed
    }

    /**
     * Records that [fileName] could not be parsed, copying its contents aside on the first failure.
     *
     * Only the first failure logs and backs up: repositories load on every read, so a corrupted file
     * would otherwise produce a log line per lookup and rewrite its own backup each time.
     */
    private fun markUnreadable(fileName: String, description: String, contents: String, reason: String?) {
        if (!unreadableFiles.add(fileName)) {
            return
        }
        plugin.logger.severe("CRITICAL: Failed to parse $description from $fileName: $reason")
        plugin.logger.severe(
            "Writes to $fileName are refused until it can be read, so that its contents are not replaced with nothing. " +
                "Repair or remove the file; no restart is needed once it parses again."
        )
        backUpUnreadableFile(fileName, contents)
    }

    private fun backUpUnreadableFile(fileName: String, contents: String) {
        try {
            val preferredBackup = File(storageDir, "$fileName.corrupted.backup")
            // An earlier backup is itself the only surviving copy of an earlier failure, so it is kept.
            val backupFile = if (preferredBackup.exists()) {
                File(storageDir, "$fileName.corrupted.${System.currentTimeMillis()}.backup")
            } else {
                preferredBackup
            }
            backupFile.writeText(contents)
            plugin.logger.warning("Unreadable file backed up to: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to back up unreadable file $fileName: ${e.message}")
        }
    }

    /**
     * Whether [fileName] last parsed as something other than valid data, and so cannot be written.
     */
    fun isUnreadable(fileName: String): Boolean = unreadableFiles.contains(fileName)

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
            // Refused rather than written: the data being written was built from a load that could not
            // read this file, so writing it would replace real records with whatever was loaded instead.
            if (unreadableFiles.contains(fileName)) {
                throw UnreadableJsonFileException(fileName)
            }
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

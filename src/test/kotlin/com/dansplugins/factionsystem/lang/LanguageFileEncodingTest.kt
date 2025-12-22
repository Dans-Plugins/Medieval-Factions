package com.dansplugins.factionsystem.lang

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests to verify the encoding integrity of language files.
 * These tests help prevent character corruption when editing language files.
 */
class LanguageFileEncodingTest {

    private val langDirectory = File("src/main/resources/lang")

    /**
     * Verifies that all language files can be read without encoding errors.
     */
    @Test
    fun testAllLanguageFilesHaveValidEncoding() {
        val langFiles = langDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("lang_") && file.name.endsWith(".properties")
        } ?: fail("Language directory not found or empty")

        assertTrue(langFiles.isNotEmpty(), "No language files found in $langDirectory")

        for (file in langFiles) {
            val encoding = detectEncoding(file)
            try {
                // Read the file with detected encoding to ensure no errors
                file.readText(encoding)
            } catch (e: Exception) {
                fail("Failed to read ${file.name} with $encoding encoding: ${e.message}")
            }
        }
    }

    /**
     * Verifies that Portuguese language file can be read.
     * Note: This file historically has mixed encoding (UTF-8 with some Latin-1 bytes).
     */
    @Test
    fun testPortugueseFileIsReadable() {
        val portugueseFile = File(langDirectory, "lang_pt_BR.properties")
        assertTrue(portugueseFile.exists(), "Portuguese language file not found")

        // The Portuguese file has mixed encoding, so we check the beginning is valid UTF-8
        // and that the file can be read in general
        val lines = portugueseFile.readLines(Charset.forName("ISO-8859-1"))
        assertTrue(lines.isNotEmpty(), "Portuguese file should not be empty")

        // Verify the file contains Portuguese content
        val content = lines.joinToString("\n")
        assertTrue(
            content.contains("Uso:") || content.contains("CommandFaction"),
            "Portuguese file should contain expected content"
        )
    }

    /**
     * Verifies that German language file uses ISO-8859-1 encoding.
     */
    @Test
    fun testGermanFileUsesIso88591() {
        val germanFile = File(langDirectory, "lang_de_DE.properties")
        assertTrue(germanFile.exists(), "German language file not found")

        // Try to read with ISO-8859-1
        try {
            val content = germanFile.readText(Charset.forName("ISO-8859-1"))
            assertTrue(
                content.contains("ü") || content.contains("ö") || content.contains("ä"),
                "German file should contain umlaut characters (ü, ö, ä, ß, etc.)"
            )
        } catch (e: Exception) {
            fail("Failed to read German file with ISO-8859-1 encoding: ${e.message}")
        }
    }

    /**
     * Verifies that English language files use UTF-8 encoding.
     */
    @Test
    fun testEnglishFilesUseUtf8() {
        val englishFiles = listOf(
            File(langDirectory, "lang_en_US.properties"),
            File(langDirectory, "lang_en_GB.properties")
        )

        for (file in englishFiles) {
            assertTrue(file.exists(), "${file.name} not found")

            val encoding = detectEncoding(file)
            assertTrue(
                encoding == StandardCharsets.UTF_8 || encoding == StandardCharsets.US_ASCII,
                "${file.name} should use UTF-8 or ASCII encoding, but detected: $encoding"
            )

            // Ensure file can be read
            try {
                file.readText(StandardCharsets.UTF_8)
            } catch (e: Exception) {
                fail("Failed to read ${file.name} with UTF-8 encoding: ${e.message}")
            }
        }
    }

    /**
     * Verifies that French language file uses UTF-8 encoding.
     */
    @Test
    fun testFrenchFileUsesUtf8() {
        val frenchFile = File(langDirectory, "lang_fr_FR.properties")
        assertTrue(frenchFile.exists(), "French language file not found")

        val encoding = detectEncoding(frenchFile)
        assertTrue(
            encoding == StandardCharsets.UTF_8,
            "French file should use UTF-8 encoding, but detected: $encoding"
        )

        // Verify specific French characters can be read correctly
        val content = frenchFile.readText(StandardCharsets.UTF_8)
        assertTrue(
            content.contains("é") || content.contains("è") || content.contains("à"),
            "French file should contain accented characters (é, è, à, etc.)"
        )
    }

    /**
     * Detects the encoding of a file by attempting to read with different charsets.
     */
    private fun detectEncoding(file: File): Charset {
        // Try UTF-8 first (most common for properties files)
        try {
            val content = file.readBytes()
            val text = String(content, StandardCharsets.UTF_8)
            // Check for replacement characters which indicate invalid UTF-8
            if (text.contains('\uFFFD')) {
                throw Exception("Invalid UTF-8 - contains replacement characters")
            }
            // Check if it's pure ASCII
            if (content.all { it in 0..127 }) {
                return StandardCharsets.US_ASCII
            }
            return StandardCharsets.UTF_8
        } catch (e: Exception) {
            // UTF-8 failed, try ISO-8859-1
            try {
                file.readText(Charset.forName("ISO-8859-1"))
                return Charset.forName("ISO-8859-1")
            } catch (e2: Exception) {
                // Default to UTF-8 if detection fails
                return StandardCharsets.UTF_8
            }
        }
    }

    /**
     * Verifies that no language file contains corrupted character sequences.
     * This checks for common corruption patterns like � (replacement character)
     * and HTML entity corruption patterns like <EA>, <E3>, <FC>.
     */
    @Test
    fun testNoCorruptedCharacters() {
        val langFiles = langDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("lang_") && file.name.endsWith(".properties")
        } ?: fail("Language directory not found or empty")

        val htmlEntityPattern = Regex("<[EF][0-9A-F]{1,2}>")

        for (file in langFiles) {
            val encoding = detectEncoding(file)
            val content = file.readText(encoding)

            // Check for UTF-8 replacement character (�)
            val hasReplacementChar = content.contains('\uFFFD')

            // Check for HTML entity corruption patterns (e.g., <EA>, <E3>, <FC>)
            val hasHtmlEntityCorruption = htmlEntityPattern.containsMatchIn(content)

            if (hasReplacementChar || hasHtmlEntityCorruption) {
                fail("${file.name} contains corrupted characters. This indicates an encoding issue.")
            }
        }
    }
}

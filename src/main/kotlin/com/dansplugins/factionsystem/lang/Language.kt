package com.dansplugins.factionsystem.lang

import com.dansplugins.factionsystem.MedievalFactions
import java.io.File
import java.net.URLClassLoader
import java.text.MessageFormat
import java.util.*

class Language(plugin: MedievalFactions, private val language: String) {

    private val resourceBundles: List<ResourceBundle>
    val locale: Locale = Locale.forLanguageTag(language)

    init {
        val languageFolder = File(plugin.dataFolder, "lang")
        if (!languageFolder.exists()) {
            languageFolder.mkdirs()
        }

        val filenames = listOf("lang_en_US", "lang_en_GB", "lang_fr_FR", "lang_de_DE", "lang_pt_BR")

        filenames.forEach { filename ->
            val filepath = "lang/$filename.properties"
            val file = File(plugin.dataFolder, filepath)
            if (!file.exists()) {
                plugin.saveResource(filepath, false)
            }
        }

        val externalUrls = arrayOf(languageFolder.toURI().toURL())
        val externalClassLoader = URLClassLoader(externalUrls)
        val externalResourceBundle = ResourceBundle.getBundle(
            "lang",
            locale,
            externalClassLoader
        )

        resourceBundles = try {
            // The jar keeps its language files under lang/, so the base name must include that folder
            val internalResourceBundle = ResourceBundle.getBundle(
                "lang.lang",
                locale,
                javaClass.classLoader
            )
            listOf(externalResourceBundle, internalResourceBundle)
        } catch (e: MissingResourceException) {
            listOf(externalResourceBundle)
        }
    }

    operator fun get(key: String, vararg params: String): String {
        val values = resourceBundles.mapNotNull { resourceBundle ->
            try {
                resourceBundle.getString(key)
            } catch (exception: MissingResourceException) {
                null
            }
        }
        // Released builds up to 10.2.0 corrupted the ISO-8859-1 language files while packaging them, replacing every
        // accented character with U+FFFD, and servers keep the copy made on first run. A value damaged that way
        // gives way to a later (bundled) one that is intact; any other on-disk value, edited or not, still wins.
        val value = values.firstOrNull { !it.contains(REPLACEMENT_CHARACTER) } ?: values.firstOrNull()
            ?: return "Missing translation for $language: $key"
        return MessageFormat.format(value, *params)
    }

    private companion object {
        const val REPLACEMENT_CHARACTER = '�'
    }
}

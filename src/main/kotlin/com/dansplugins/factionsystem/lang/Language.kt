package com.dansplugins.factionsystem.lang

import com.dansplugins.factionsystem.MedievalFactions
import java.io.File
import java.net.URLClassLoader
import java.text.MessageFormat
import java.util.*

class Language(plugin: MedievalFactions, private val language: String) {

    private val resourceBundles: List<ResourceBundle>
    val locale = Locale.forLanguageTag(language)

    init {
        val languageFolder = File(plugin.dataFolder, "lang")
        if (!languageFolder.exists()) {
            languageFolder.mkdirs()
        }

        val en_us_filename = "lang/lang_en_US.properties"
        var en_us_file = File(en_us_filename)
        if (!en_us_file.exists()) {
            plugin.saveResource(en_us_filename, false)
        }

        val en_gb_filename = "lang/lang_en_GB.properties"
        var en_gb_file = File(en_gb_filename)
        if (!en_gb_file.exists()) {
            plugin.saveResource(en_gb_filename, false)
        }

        val fr_fr_filename = "lang/lang_fr_FR.properties"
        var fr_fr_file = File(fr_fr_filename)
        if (!fr_fr_file.exists()) {
            plugin.saveResource(fr_fr_filename, false)
        }

        val de_de_filename = "lang/lang_de_DE.properties"
        var de_de_file = File(de_de_filename)
        if (!de_de_file.exists()) {
            plugin.saveResource(de_de_filename, false)
        }
        plugin.saveResource("lang/lang_de_DE.properties", false)

        val externalUrls = arrayOf(languageFolder.toURI().toURL())
        val externalClassLoader = URLClassLoader(externalUrls)
        val externalResourceBundle = ResourceBundle.getBundle(
            "lang",
            locale,
            externalClassLoader
        )
        val internalResourceBundle = ResourceBundle.getBundle(
            "lang/lang",
            locale
        )
        resourceBundles = listOf(externalResourceBundle, internalResourceBundle)
    }

    operator fun get(key: String, vararg params: String) =
        resourceBundles.firstNotNullOfOrNull { resourceBundle ->
            try {
                MessageFormat.format(resourceBundle.getString(key), *params)
            } catch (exception: MissingResourceException) {
                null
            }
        } ?: "Missing translation for $language: $key"
}

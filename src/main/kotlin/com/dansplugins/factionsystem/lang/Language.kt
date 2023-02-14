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

        val filenames = listOf("lang_en_US", "lang_en_GB", "lang_fr_FR", "lang_de_DE")

        filenames.forEach {
            val filename = "lang/$it.properties"
            val file = File(plugin.dataFolder, filename)
            if (!file.exists()) {
                plugin.saveResource(filename, false)
            }
        }

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

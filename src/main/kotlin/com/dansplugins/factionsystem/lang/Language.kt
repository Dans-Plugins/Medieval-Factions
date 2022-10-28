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
            plugin.saveResource("lang/lang_en_US.properties", false)
            plugin.saveResource("lang/lang_en_GB.properties", false)
            plugin.saveResource("lang/lang_de.properties", false)
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
        } ?: "Missing translation for ${language}: $key"

}
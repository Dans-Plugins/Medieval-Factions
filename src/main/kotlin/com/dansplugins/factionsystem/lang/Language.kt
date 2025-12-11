package com.dansplugins.factionsystem.lang

import com.dansplugins.factionsystem.MedievalFactions
import java.io.File
import java.net.URLClassLoader
import java.text.MessageFormat
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

class Language(
    plugin: MedievalFactions,
    private val language: String,
) {
    private val resourceBundles: List<ResourceBundle>
    val locale: Locale = Locale.forLanguageTag(language)

    init {
        val languageFolder = File(plugin.dataFolder, "lang")
        if (!languageFolder.exists()) {
            languageFolder.mkdirs()
        }

        val filenames = listOf("lang_en_US", "lang_en_GB", "lang_fr_FR", "lang_de_DE")

        filenames.forEach { filename ->
            val filepath = "lang/$filename.properties"
            val file = File(plugin.dataFolder, filepath)
            if (!file.exists()) {
                plugin.saveResource(filepath, false)
            }
        }

        val externalUrls = arrayOf(languageFolder.toURI().toURL())
        val externalClassLoader = URLClassLoader(externalUrls)
        val externalResourceBundle =
            ResourceBundle.getBundle(
                "lang",
                locale,
                externalClassLoader,
            )

        resourceBundles =
            try {
                val internalResourceBundle =
                    ResourceBundle.getBundle(
                        "lang",
                        locale,
                    )
                listOf(externalResourceBundle, internalResourceBundle)
            } catch (e: MissingResourceException) {
                listOf(externalResourceBundle)
            }
    }

    operator fun get(
        key: String,
        vararg params: String,
    ) = resourceBundles.firstNotNullOfOrNull { resourceBundle ->
        try {
            MessageFormat.format(resourceBundle.getString(key), *params)
        } catch (exception: MissingResourceException) {
            null
        }
    } ?: "Missing translation for $language: $key"
}

package com.dansplugins.factionsystem.lang

import java.text.MessageFormat
import java.util.*

class Language(private val resourceBundle: ResourceBundle) {

    operator fun get(key: String, vararg params: String): String = try {
        MessageFormat.format(resourceBundle.getString(key), *params)
    } catch (exception: MissingResourceException) {
        "Missing translation for ${resourceBundle.locale.toLanguageTag()}: $key"
    }

}
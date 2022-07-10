package com.dansplugins.factionsystem.lang

import java.text.MessageFormat
import java.util.*

class Language(private val resourceBundle: ResourceBundle) {

    operator fun get(key: String, vararg params: String): String = MessageFormat.format(resourceBundle.getString(key), *params)

}
package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.lang.Language

fun coerceBoolean(plugin: MedievalFactions) = { value: String? ->
    parseFlagBoolean(value, plugin.language["FlagValueTrue"], plugin.language["FlagValueFalse"])
        ?.let(::MfFlagValueCoercionSuccess)
        ?: MfFlagValueCoercionFailure(plugin.language["FactionFlagBooleanCoercionFailed"])
}

/**
 * Reads a boolean flag value. `true` and `false` are always accepted, exactly as before, so
 * commands written for any language keep working. The words for true and false in the
 * server's language ([trueWord] and [falseWord], from `FlagValueTrue` and `FlagValueFalse`)
 * are also accepted, ignoring case and surrounding spaces.
 */
fun parseFlagBoolean(value: String?, trueWord: String, falseWord: String): Boolean? {
    if (value == null) return null
    value.toBooleanStrictOrNull()?.let { return it }
    val trimmed = value.trim()
    return when {
        trimmed.isEmpty() -> null
        trimmed.equals(trueWord.trim(), ignoreCase = true) -> true
        trimmed.equals(falseWord.trim(), ignoreCase = true) -> false
        else -> null
    }
}

/**
 * The text shown to players for a flag value: a boolean in the server's language
 * (`FlagValueTrue` / `FlagValueFalse`), anything else as it is stored.
 */
fun displayFlagValue(language: Language, value: Any?): String = when (value) {
    true -> language["FlagValueTrue"]
    false -> language["FlagValueFalse"]
    else -> value.toString()
}

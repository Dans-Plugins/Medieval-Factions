package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions

fun coerceBoolean(plugin: MedievalFactions) = { value: String? ->
    value?.toBooleanStrictOrNull()?.let(::MfFlagValueCoercionSuccess)
        ?: MfFlagValueCoercionFailure(plugin.language["FactionFlagBooleanCoercionFailed"])
}

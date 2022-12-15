package com.dansplugins.factionsystem.faction.permission

import com.dansplugins.factionsystem.faction.MfFaction

data class MfFactionPermission(
    val name: String,
    val translate: (faction: MfFaction) -> String,
    val default: Boolean
) {

    constructor(name: String, translation: String, default: Boolean) : this(name, { translation }, default)

    override fun toString() = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MfFactionPermission

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

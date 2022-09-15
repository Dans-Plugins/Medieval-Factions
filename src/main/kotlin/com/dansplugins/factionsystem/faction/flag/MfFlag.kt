package com.dansplugins.factionsystem.faction.flag

data class MfFlag<T: Any?>(
    val name: String,
    private val default: () -> T
) {

    constructor(name: String, defaultValue: T): this(name, { defaultValue })

    val defaultValue: T
        get() = default()
}
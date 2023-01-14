package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import kotlin.reflect.KClass

data class MfFlag<T : Any>(
    val name: String,
    val type: KClass<T>,
    private val default: () -> T,
    val coerce: (value: String) -> MfFlagValueCoercionResult,
    val validate: (value: T) -> MfFlagValidationResult = { MfFlagValidationSuccess }
) {

    companion object {
        fun string(
            name: String,
            default: () -> String,
            validate: (value: String) -> MfFlagValidationResult = { MfFlagValidationSuccess }
        ): MfFlag<String> = MfFlag(
            name,
            String::class,
            default,
            ::MfFlagValueCoercionSuccess,
            validate
        )

        fun string(
            name: String,
            defaultValue: String,
            validate: (value: String) -> MfFlagValidationResult = { MfFlagValidationSuccess }
        ): MfFlag<String> = string(
            name,
            { defaultValue },
            validate
        )

        fun boolean(
            plugin: MedievalFactions,
            name: String,
            default: () -> Boolean,
            validate: (value: Boolean) -> MfFlagValidationResult = { MfFlagValidationSuccess }
        ): MfFlag<Boolean> = MfFlag(
            name,
            Boolean::class,
            default,
            coerceBoolean(plugin),
            validate
        )

        fun boolean(
            plugin: MedievalFactions,
            name: String,
            defaultValue: Boolean,
            validate: (value: Boolean) -> MfFlagValidationResult = { MfFlagValidationSuccess }
        ): MfFlag<Boolean> = boolean(
            plugin,
            name,
            { defaultValue },
            validate
        )
    }

    constructor(
        name: String,
        type: KClass<T>,
        defaultValue: T,
        coerce: (value: String?) -> MfFlagValueCoercionResult
    ) : this(
        name,
        type,
        { defaultValue },
        coerce,
        { MfFlagValidationSuccess }
    )

    fun withValidation(validate: (value: T) -> MfFlagValidationResult) = copy(validate = validate)

    val defaultValue: T
        get() = default()
}

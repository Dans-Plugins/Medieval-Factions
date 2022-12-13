package com.dansplugins.factionsystem.faction.flag

sealed interface MfFlagValueCoercionResult

class MfFlagValueCoercionSuccess<T : Any>(val value: T) : MfFlagValueCoercionResult
class MfFlagValueCoercionFailure(val failureMessage: String) : MfFlagValueCoercionResult

fun MfFlagValueCoercionResult.onFailure(block: (MfFlagValueCoercionFailure) -> Nothing) {
    if (this is MfFlagValueCoercionFailure) {
        block(this)
    }
}

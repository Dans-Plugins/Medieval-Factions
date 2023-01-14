package com.dansplugins.factionsystem.faction.flag

sealed interface MfFlagValidationResult

object MfFlagValidationSuccess : MfFlagValidationResult
data class MfFlagValidationFailure(
    val failureMessage: String
) : MfFlagValidationResult

fun MfFlagValidationResult.onFailure(block: (MfFlagValidationFailure) -> Nothing) {
    if (this is MfFlagValidationFailure) {
        block(this)
    }
}

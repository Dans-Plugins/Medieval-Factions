package com.dansplugins.factionsystem.locks

// This enum should contain the result of an unlock attempt.
enum class MfUnlockResult {
    // The block was successfully unlocked.
    SUCCESS,

    // The block was not locked.
    NOT_LOCKED,

    // The unlock operation failed.
    FAILURE
}

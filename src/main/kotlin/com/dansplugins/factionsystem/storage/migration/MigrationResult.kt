package com.dansplugins.factionsystem.storage.migration

/**
 * Result of a storage migration operation
 */
data class MigrationResult(
    val success: Boolean,
    val itemsMigrated: Int,
    val durationMs: Long,
    val message: String,
    val error: Throwable? = null
)

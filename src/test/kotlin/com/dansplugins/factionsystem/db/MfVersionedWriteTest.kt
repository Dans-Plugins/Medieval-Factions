package com.dansplugins.factionsystem.db

import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the decision logic in [MfVersionedWrite], independent of any database. The
 * repository contract tests exercise it end to end on H2 and MariaDB; these pin each branch,
 * including the insert race that a single-threaded contract test cannot produce.
 */
class MfVersionedWriteTest {

    private class Calls(
        private val updateCount: Int = 0,
        private val rowExistsResults: List<Boolean> = listOf(false),
        private val insertFailure: Exception? = null
    ) {
        var updates = 0
        var rowExistsChecks = 0
        var inserts = 0

        fun update(): Int {
            updates++
            return updateCount
        }

        fun rowExists(): Boolean {
            val result = rowExistsResults[minOf(rowExistsChecks, rowExistsResults.lastIndex)]
            rowExistsChecks++
            return result
        }

        fun insert() {
            inserts++
            insertFailure?.let { throw it }
        }

        fun write(): Boolean = MfVersionedWrite.write(::update, ::rowExists, ::insert)
    }

    @Test
    fun write_returnsTrueWithoutCheckingOrInserting_whenTheUpdateMatchesTheExpectedVersion() {
        val calls = Calls(updateCount = 1)

        assertTrue(calls.write())
        assertEquals(1, calls.updates)
        assertEquals(0, calls.rowExistsChecks)
        assertEquals(0, calls.inserts)
    }

    @Test
    fun write_treatsAnyPositiveUpdateCountAsApplied() {
        val calls = Calls(updateCount = 2)

        assertTrue(calls.write())
        assertEquals(0, calls.rowExistsChecks)
        assertEquals(0, calls.inserts)
    }

    @Test
    fun write_returnsFalseWithoutInserting_whenNothingWasUpdatedAndTheRowExists() {
        // The row exists at another version: a stale write, and nothing may be written.
        val calls = Calls(updateCount = 0, rowExistsResults = listOf(true))

        assertFalse(calls.write())
        assertEquals(1, calls.rowExistsChecks)
        assertEquals(0, calls.inserts)
    }

    @Test
    fun write_insertsANewRowOnce_whenNothingWasUpdatedAndTheRowDoesNotExist() {
        val calls = Calls(updateCount = 0, rowExistsResults = listOf(false))

        assertTrue(calls.write())
        assertEquals(1, calls.updates)
        assertEquals(1, calls.rowExistsChecks)
        assertEquals(1, calls.inserts)
    }

    @Test
    fun write_returnsFalse_whenTheInsertFailsBecauseAnotherWriterCreatedTheRowFirst() {
        val calls = Calls(
            updateCount = 0,
            rowExistsResults = listOf(false, true),
            insertFailure = DataAccessException("duplicate key")
        )

        assertFalse(calls.write())
        assertEquals(1, calls.inserts)
        assertEquals(2, calls.rowExistsChecks)
    }

    @Test
    fun write_rethrowsTheSameException_whenTheInsertFailsAndTheRowStillDoesNotExist() {
        // A failure that is not an id collision (another unique key, a constraint) must surface.
        val failure = DataAccessException("unique constraint on name")
        val calls = Calls(updateCount = 0, rowExistsResults = listOf(false, false), insertFailure = failure)

        val thrown = assertThrows(DataAccessException::class.java) { calls.write() }

        assertSame(failure, thrown)
        assertEquals(2, calls.rowExistsChecks)
    }

    @Test
    fun write_propagatesANonDataAccessInsertFailureWithoutRecheckingTheRow() {
        val failure = IllegalStateException("not a database error")
        val calls = Calls(updateCount = 0, rowExistsResults = listOf(false, true), insertFailure = failure)

        val thrown = assertThrows(IllegalStateException::class.java) { calls.write() }

        assertSame(failure, thrown)
        assertEquals(1, calls.rowExistsChecks)
    }

    @Test
    fun write_propagatesAnUpdateFailureWithoutCheckingOrInserting() {
        val failure = DataAccessException("connection lost")
        var rowExistsChecks = 0
        var inserts = 0

        val thrown = assertThrows(DataAccessException::class.java) {
            MfVersionedWrite.write(
                update = { throw failure },
                rowExists = { rowExistsChecks++; false },
                insert = { inserts++ }
            )
        }

        assertSame(failure, thrown)
        assertEquals(0, rowExistsChecks)
        assertEquals(0, inserts)
    }
}

package com.dansplugins.factionsystem.db

import org.jooq.exception.DataAccessException

/**
 * Writes one row guarded by an optimistic-lock `version` column and reports whether the write
 * was applied (`true`) or the stored row's version did not match (`false`, a stale write).
 *
 * The repositories used to do this with one `INSERT … ON CONFLICT DO UPDATE … WHERE version = ?`
 * and treat an update count of 0 as a stale write. That holds on H2, but on MariaDB and MySQL
 * jOOQ renders that statement as `INSERT … ON DUPLICATE KEY UPDATE` with `CASE` expressions on
 * the version, and the driver counts a row that was matched but left unchanged as 1 affected
 * row. A stale write was therefore skipped and reported as a success (#2076).
 *
 * The write is split into statements whose results mean the same thing on every database:
 *
 * 1. [update] runs `UPDATE … SET …, version = version + 1 WHERE id = ? AND version = ?`. A plain
 *    update reports the rows its WHERE clause matched (MariaDB's default found-rows mode, and H2)
 *    or the rows it changed (`useAffectedRows`). It always changes the version, so both are 1
 *    when the stored version is the expected one and 0 otherwise.
 * 2. Only if nothing was updated, [rowExists] tells a stale write (the row exists at another
 *    version) apart from a new row, which [insert] then inserts.
 * 3. If [insert] fails and a row with that id now exists, another writer created it first, so
 *    the write is stale. Any other failure, such as a different unique key, is rethrown unchanged.
 *
 * The outcomes are the ones the single upsert had on H2: a new row is inserted at version 1, a
 * row at the expected version is updated, and anything else is a conflict. Nothing is written in
 * the conflict case. [rowExists] must be a locking (current) read, such as `SELECT … FOR UPDATE`,
 * so that inside a transaction it sees rows committed after the transaction's snapshot was taken.
 */
internal object MfVersionedWrite {

    fun write(update: () -> Int, rowExists: () -> Boolean, insert: () -> Unit): Boolean {
        if (update() > 0) return true
        if (rowExists()) return false
        try {
            insert()
        } catch (exception: DataAccessException) {
            if (rowExists()) return false
            throw exception
        }
        return true
    }
}

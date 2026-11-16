package com.dansplugins.factionsystem.command.faction.claim

import com.dansplugins.factionsystem.faction.MfFactionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MfFactionClaimShowCommandTest {
    private val alpha = MfFactionId.generate()
    private val beta = MfFactionId.generate()

    @Test
    fun loneClaimIsOutlinedOnAllFourSides() {
        val owners = mapOf((0 to 0) to alpha)

        val columns = MfFactionClaimShowCommand.borderColumns(0, 0) { x, z -> owners[x to z] }

        // 4 edges of 16 columns share 4 corners
        assertEquals(60, columns.size)
        assertTrue((0 to 0) in columns && (15 to 15) in columns && (0 to 15) in columns && (15 to 0) in columns)
        assertTrue((7 to 7) !in columns)
    }

    @Test
    fun edgeSharedWithTheSameFactionIsNotMarked() {
        val owners = mapOf((0 to 0) to alpha, (1 to 0) to alpha)

        val columns = MfFactionClaimShowCommand.borderColumns(0, 0) { x, z -> owners[x to z] }

        assertTrue((15 to 7) !in columns)
        assertTrue((0 to 7) in columns)
    }

    @Test
    fun edgeAgainstAnotherFactionIsMarked() {
        val owners = mapOf((-1 to 2) to alpha, (0 to 2) to beta)

        val columns = MfFactionClaimShowCommand.borderColumns(-1, 2) { x, z -> owners[x to z] }

        // Chunk -1 spans block x -16..-1; its eastern edge at x = -1 faces beta's chunk
        assertTrue((-1 to 40) in columns)
    }

    @Test
    fun unclaimedChunkHasNoBorder() {
        assertEquals(emptySet<Pair<Int, Int>>(), MfFactionClaimShowCommand.borderColumns(0, 0) { _, _ -> null })
    }
}

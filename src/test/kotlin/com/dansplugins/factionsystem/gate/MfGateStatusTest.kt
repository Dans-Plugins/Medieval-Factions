package com.dansplugins.factionsystem.gate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MfGateStatusTest {

    @Test
    fun testOpenStatus() {
        // execute
        val status = MfGateStatus.OPEN

        // verify
        assertEquals("OPEN", status.name)
    }

    @Test
    fun testOpeningStatus() {
        // execute
        val status = MfGateStatus.OPENING

        // verify
        assertEquals("OPENING", status.name)
    }

    @Test
    fun testClosedStatus() {
        // execute
        val status = MfGateStatus.CLOSED

        // verify
        assertEquals("CLOSED", status.name)
    }

    @Test
    fun testClosingStatus() {
        // execute
        val status = MfGateStatus.CLOSING

        // verify
        assertEquals("CLOSING", status.name)
    }

    @Test
    fun testAllStatusesPresent() {
        // execute
        val statuses = MfGateStatus.values()

        // verify - ensure all 4 statuses exist
        assertEquals(4, statuses.size)
        assertTrue(statuses.contains(MfGateStatus.OPEN))
        assertTrue(statuses.contains(MfGateStatus.OPENING))
        assertTrue(statuses.contains(MfGateStatus.CLOSED))
        assertTrue(statuses.contains(MfGateStatus.CLOSING))
    }

    @Test
    fun testValueOf() {
        // execute & verify
        assertEquals(MfGateStatus.OPEN, MfGateStatus.valueOf("OPEN"))
        assertEquals(MfGateStatus.OPENING, MfGateStatus.valueOf("OPENING"))
        assertEquals(MfGateStatus.CLOSED, MfGateStatus.valueOf("CLOSED"))
        assertEquals(MfGateStatus.CLOSING, MfGateStatus.valueOf("CLOSING"))
    }

    @Test
    fun testEnumOrdinal() {
        // execute & verify - test ordering
        assertEquals(0, MfGateStatus.OPEN.ordinal)
        assertEquals(1, MfGateStatus.OPENING.ordinal)
        assertEquals(2, MfGateStatus.CLOSED.ordinal)
        assertEquals(3, MfGateStatus.CLOSING.ordinal)
    }

    @Test
    fun testStatusTransitions() {
        // prepare - typical gate state transitions
        val closedToOpening = MfGateStatus.CLOSED to MfGateStatus.OPENING
        val openingToOpen = MfGateStatus.OPENING to MfGateStatus.OPEN
        val openToClosing = MfGateStatus.OPEN to MfGateStatus.CLOSING
        val closingToClosed = MfGateStatus.CLOSING to MfGateStatus.CLOSED

        // verify - transitions are valid enum values
        assertEquals(MfGateStatus.CLOSED, closedToOpening.first)
        assertEquals(MfGateStatus.OPENING, closedToOpening.second)
        assertEquals(MfGateStatus.OPENING, openingToOpen.first)
        assertEquals(MfGateStatus.OPEN, openingToOpen.second)
        assertEquals(MfGateStatus.OPEN, openToClosing.first)
        assertEquals(MfGateStatus.CLOSING, openToClosing.second)
        assertEquals(MfGateStatus.CLOSING, closingToClosed.first)
        assertEquals(MfGateStatus.CLOSED, closingToClosed.second)
    }
}

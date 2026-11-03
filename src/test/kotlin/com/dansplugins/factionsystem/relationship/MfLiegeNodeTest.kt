package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MfLiegeNodeTest {

    private val vassal = MfFactionId("vassal")
    private val liege = MfFactionId("liege")
    private val highLiege = MfFactionId("high-liege")
    private val outsider = MfFactionId("outsider")

    // vassal -> liege -> high-liege
    private val topNode = MfLiegeNode(highLiege, null)
    private val chain = MfLiegeNode(vassal, MfLiegeNode(liege, topNode))

    @Test
    fun testContainsDirectLiege() {
        assertTrue(chain.contains(liege))
    }

    @Test
    fun testContainsIndirectLiege() {
        assertTrue(chain.contains(highLiege))
    }

    @Test
    fun testContainsExcludesStartingFaction() {
        assertFalse(chain.contains(vassal))
    }

    @Test
    fun testContainsExcludesUnrelatedFaction() {
        assertFalse(chain.contains(outsider))
    }

    @Test
    fun testContainsWithoutLiegeIsFalse() {
        val independent = MfLiegeNode(vassal, null)

        assertFalse(independent.contains(vassal))
        assertFalse(independent.contains(outsider))
    }

    @Test
    fun testLastReturnsTopOfChain() {
        assertSame(topNode, chain.last())
        assertEquals(highLiege, chain.last().factionId)
    }

    @Test
    fun testLastWithoutLiegeReturnsSelf() {
        val independent = MfLiegeNode(vassal, null)

        assertSame(independent, independent.last())
    }

    @Test
    fun testContainsIsCallableFromJava() {
        // MfFactionId is a value class, so without @JvmName the Kotlin compiler mangles the
        // method name and Java plugins can no longer call it (see #1550).
        val method = MfLiegeNode::class.java.getMethod("contains", String::class.java)

        assertEquals(Boolean::class.javaPrimitiveType, method.returnType)
    }
}

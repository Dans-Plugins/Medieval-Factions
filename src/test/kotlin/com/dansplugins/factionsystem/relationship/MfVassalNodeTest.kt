package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MfVassalNodeTest {

    private val liege = MfFactionId("liege")
    private val vassalA = MfFactionId("vassal-a")
    private val vassalB = MfFactionId("vassal-b")
    private val subVassal = MfFactionId("sub-vassal")
    private val outsider = MfFactionId("outsider")

    // liege
    // ├── vassal-a
    // │   └── sub-vassal
    // └── vassal-b
    private val tree = MfVassalNode(
        liege,
        listOf(
            MfVassalNode(vassalA, listOf(MfVassalNode(subVassal, emptyList()))),
            MfVassalNode(vassalB, emptyList())
        )
    )

    @Test
    fun testContainsDirectVassal() {
        assertTrue(tree.contains(vassalA))
        assertTrue(tree.contains(vassalB))
    }

    @Test
    fun testContainsIndirectVassal() {
        assertTrue(tree.contains(subVassal))
    }

    @Test
    fun testContainsExcludesRootFaction() {
        assertFalse(tree.contains(liege))
    }

    @Test
    fun testContainsExcludesUnrelatedFaction() {
        assertFalse(tree.contains(outsider))
    }

    @Test
    fun testContainsOnLeafIsFalse() {
        val leaf = MfVassalNode(liege, emptyList())

        assertFalse(leaf.contains(liege))
        assertFalse(leaf.contains(outsider))
    }

    @Test
    fun testContainsDoesNotLookUpTheTree() {
        val subtree = tree.vassals.first { it.factionId == vassalA }

        assertTrue(subtree.contains(subVassal))
        assertFalse(subtree.contains(liege))
        assertFalse(subtree.contains(vassalB))
    }

    @Test
    fun testMapVisitsEveryVassalOnceDescendantsFirstAndExcludesRoot() {
        val result = tree.map { it.value }

        assertEquals(listOf("sub-vassal", "vassal-a", "vassal-b"), result)
    }

    @Test
    fun testMapOnLeafIsEmpty() {
        assertEquals(emptyList<String>(), MfVassalNode(liege, emptyList()).map { it.value })
    }

    @Test
    fun testFlatMapConcatenatesResultsInSameOrderAsMap() {
        val result = tree.flatMap { listOf(it.value, it.value.uppercase()) }

        assertEquals(
            listOf("sub-vassal", "SUB-VASSAL", "vassal-a", "VASSAL-A", "vassal-b", "VASSAL-B"),
            result
        )
    }

    @Test
    fun testMapNotNullDropsNullResults() {
        val result = tree.mapNotNull { if (it == vassalA) null else it.value }

        assertEquals(listOf("sub-vassal", "vassal-b"), result)
    }

    @Test
    fun testMapNotNullKeepsDescendantsOfFactionMappedToNull() {
        val result = tree.mapNotNull { if (it == vassalA) null else it }

        assertTrue(result.contains(subVassal))
    }

    @Test
    fun testForEachVisitsEveryVassalInSameOrderAsMap() {
        val visited = mutableListOf<MfFactionId>()

        tree.forEach { visited.add(it) }

        assertEquals(listOf(subVassal, vassalA, vassalB), visited)
    }

    @Test
    fun testForEachOnLeafVisitsNothing() {
        val visited = mutableListOf<MfFactionId>()

        MfVassalNode(liege, emptyList()).forEach { visited.add(it) }

        assertEquals(emptyList<MfFactionId>(), visited)
    }

    @Test
    fun testContainsIsCallableFromJava() {
        // MfFactionId is a value class, so without @JvmName the Kotlin compiler mangles the
        // method name and Java plugins can no longer call it (see #1550).
        val method = MfVassalNode::class.java.getMethod("contains", String::class.java)

        assertEquals(Boolean::class.javaPrimitiveType, method.returnType)
    }

    @Test
    fun testFactionIdIsExposedAsGetFactionId() {
        val method = MfVassalNode::class.java.getMethod("getFactionId")

        assertEquals(String::class.java, method.returnType)
    }
}

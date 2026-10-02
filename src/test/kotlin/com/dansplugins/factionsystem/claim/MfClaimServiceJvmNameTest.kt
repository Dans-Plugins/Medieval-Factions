package com.dansplugins.factionsystem.claim

import kotlin.test.Test
import kotlin.test.assertEquals

class MfClaimServiceJvmNameTest {

    @Test
    fun testGetClaimsByFactionIdIsCallableFromJava() {
        // MfFactionId is a value class, so without @JvmName the Kotlin compiler mangles the
        // method name and Java plugins can no longer call it (see #1550).
        val method = MfClaimService::class.java.getMethod("getClaimsByFactionId", String::class.java)

        assertEquals(List::class.java, method.returnType)
    }

    @Test
    fun testClaimsIsExposedAsGetClaims() {
        val method = MfClaimService::class.java.getMethod("getClaims")

        assertEquals(List::class.java, method.returnType)
    }
}

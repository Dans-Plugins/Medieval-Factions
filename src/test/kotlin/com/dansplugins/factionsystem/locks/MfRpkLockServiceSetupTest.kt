package com.dansplugins.factionsystem.locks

import com.dansplugins.factionsystem.MedievalFactions
import com.rpkit.core.service.Service
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MfRpkLockServiceSetupTest {

    // Shape of com.rpkit.core.service.Service in RPKit 2.2.x, whose getPlugin() returns RPKPlugin.
    interface LegacyRpkPlugin
    interface LegacyService {
        fun getPlugin(): LegacyRpkPlugin
    }

    // Shape of com.rpkit.core.service.Service in RPKit 2.3.0 and later.
    interface CurrentService {
        fun getPlugin(): Any
    }

    interface NoPluginService

    private var registerCalls = 0
    private var incompatibleCalls = 0
    private var incompatibleError: Throwable? = null

    private fun setUp(serviceInterface: Class<*>, register: () -> Unit = { registerCalls++ }) =
        MfRpkLockServiceSetup.setUp(
            serviceInterface,
            MedievalFactions::class.java,
            onIncompatible = { error ->
                incompatibleCalls++
                incompatibleError = error
            },
            register = register
        )

    @Test
    fun setUp_WithLegacyRpkitServiceInterface_ShouldSkipRegistrationAndWarn() {
        val result = setUp(LegacyService::class.java)

        assertFalse(result)
        assertEquals(0, registerCalls)
        assertEquals(1, incompatibleCalls)
        assertNull(incompatibleError)
    }

    @Test
    fun setUp_WithCurrentRpkitServiceInterface_ShouldRegister() {
        val result = setUp(CurrentService::class.java)

        assertTrue(result)
        assertEquals(1, registerCalls)
        assertEquals(0, incompatibleCalls)
    }

    @Test
    fun setUp_WithBundledRpkitServiceInterface_ShouldRegister() {
        val result = setUp(Service::class.java)

        assertTrue(result)
        assertEquals(1, registerCalls)
    }

    @Test
    fun setUp_WhenRegistrationThrowsAbstractMethodError_ShouldNotPropagateAndWarn() {
        val error = AbstractMethodError("getPlugin()")

        val result = setUp(CurrentService::class.java) { throw error }

        assertFalse(result)
        assertEquals(1, incompatibleCalls)
        assertSame(error, incompatibleError)
    }

    @Test
    fun isCompatible_WithoutGetPluginMethod_ShouldReturnFalse() {
        assertFalse(MfRpkLockServiceSetup.isCompatible(NoPluginService::class.java, MedievalFactions::class.java))
    }
}

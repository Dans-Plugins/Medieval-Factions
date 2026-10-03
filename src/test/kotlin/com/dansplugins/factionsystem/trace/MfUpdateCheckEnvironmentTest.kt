package com.dansplugins.factionsystem.trace

import com.dansplugins.factionsystem.update.MfUpdateCheck
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.function.Function
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The update check reads TRACE_USAGE_REPORTING and DO_NOT_TRACK through the
 * vendored trace client, so it lives in the client's package to reach the
 * client's environment seam (package-private) without a real environment.
 */
class MfUpdateCheckEnvironmentTest {

    private val environment = HashMap<String, String>()
    private lateinit var realEnvironment: Function<String, String>

    @BeforeEach
    fun isolateEnvironment() {
        realEnvironment = TraceClient.environment
        TraceClient.environment = Function { environment[it] }
    }

    @AfterEach
    fun restoreEnvironment() {
        TraceClient.environment = realEnvironment
    }

    @Test
    fun testNoEnvironmentDoesNotOptOut() {
        assertFalse(MfUpdateCheck.environmentOptsOut())
    }

    @Test
    fun testTraceUsageReportingOffOptsOut() {
        for (off in listOf("off", "false", "0", "no", "FALSE", " Off ")) {
            environment.clear()
            environment["TRACE_USAGE_REPORTING"] = off
            assertTrue(MfUpdateCheck.environmentOptsOut(), "TRACE_USAGE_REPORTING=$off should opt out")
        }
    }

    @Test
    fun testDoNotTrackOptsOut() {
        for (yes in listOf("1", "true", "yes", "TRUE")) {
            environment.clear()
            environment["DO_NOT_TRACK"] = yes
            assertTrue(MfUpdateCheck.environmentOptsOut(), "DO_NOT_TRACK=$yes should opt out")
        }
    }

    @Test
    fun testFalsyValuesDoNotOptOut() {
        environment["TRACE_USAGE_REPORTING"] = "on"
        environment["DO_NOT_TRACK"] = "0"
        assertFalse(MfUpdateCheck.environmentOptsOut())
    }
}

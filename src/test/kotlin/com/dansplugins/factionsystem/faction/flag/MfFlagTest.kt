package com.dansplugins.factionsystem.faction.flag

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.lang.Language
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MfFlagTest {

    @Test
    fun stringFlagWithoutValidationAcceptsAnyValue() {
        val flag = MfFlag.string("motto", "Onwards")

        assertEquals("Onwards", flag.defaultValue)
        assertEquals(MfFlagValidationSuccess, flag.validate(""))
    }

    @Test
    fun withValidationReplacesOnlyTheValidator() {
        val flag = MfFlag.string("motto", "Onwards")
            .withValidation { if (it.isEmpty()) MfFlagValidationFailure("empty") else MfFlagValidationSuccess }

        assertEquals("motto", flag.name)
        assertEquals("Onwards", flag.defaultValue)
        assertEquals(MfFlagValidationFailure("empty"), flag.validate(""))
        assertEquals(MfFlagValidationSuccess, flag.validate("x"))
    }

    @Test
    fun booleanCoercionRejectsNull() {
        val plugin = mock(MedievalFactions::class.java)
        val language = mock(Language::class.java) { invocation -> invocation.arguments.firstOrNull() as? String ?: "" }
        `when`(plugin.language).thenReturn(language)

        val result = coerceBoolean(plugin)(null)

        assertIs<MfFlagValueCoercionFailure>(result)
        assertEquals("FactionFlagBooleanCoercionFailed", result.failureMessage)
    }

    @Test
    fun onFailureRunsTheBlockOnlyForAFailure() {
        MfFlagValidationSuccess.onFailure { throw AssertionError("not a failure") }
        MfFlagValueCoercionSuccess(true).onFailure { throw AssertionError("not a failure") }

        val validationError = assertFailsWith<IllegalStateException> {
            MfFlagValidationFailure("bad").onFailure { throw IllegalStateException(it.failureMessage) }
        }
        val coercionError = assertFailsWith<IllegalStateException> {
            MfFlagValueCoercionFailure("worse").onFailure { throw IllegalStateException(it.failureMessage) }
        }
        assertEquals("bad", validationError.message)
        assertEquals("worse", coercionError.message)
    }
}

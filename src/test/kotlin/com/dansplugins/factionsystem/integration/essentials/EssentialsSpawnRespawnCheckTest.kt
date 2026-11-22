package com.dansplugins.factionsystem.integration.essentials

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EssentialsSpawnRespawnCheckTest {

    @Test
    fun `none and lowest leave the faction home in charge`() {
        assertFalse(EssentialsSpawnRespawnCheck.overridesFactionHome("none"))
        assertFalse(EssentialsSpawnRespawnCheck.overridesFactionHome("lowest"))
        assertFalse(EssentialsSpawnRespawnCheck.overridesFactionHome(" LOWEST "))
    }

    @Test
    fun `low and above, missing and unknown values override the faction home`() {
        listOf("low", "normal", "high", "highest", "monitor", "banana", "", null).forEach {
            assertTrue(EssentialsSpawnRespawnCheck.overridesFactionHome(it), "value: $it")
        }
    }

    @Test
    fun `reads the priority from the EssentialsX config file`(@TempDir dir: File) {
        val file = File(dir, "config.yml")
        file.writeText("spawn-on-join: false\nrespawn-listener-priority: high\n")

        assertEquals("high", EssentialsSpawnRespawnCheck.readPriority(file))
    }

    @Test
    fun `a missing EssentialsX config file reads as no value`(@TempDir dir: File) {
        assertNull(EssentialsSpawnRespawnCheck.readPriority(File(dir, "config.yml")))
    }

    @Test
    fun `the warning names the setting and the fix`() {
        val message = EssentialsSpawnRespawnCheck.warning("high")

        assertTrue(message.contains("respawn-listener-priority: high"))
        assertTrue(message.contains("set respawn-listener-priority to none"))
    }
}

package com.dansplugins.factionsystem.api

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.logging.Logger

class MfApiServerTest {

    private lateinit var plugin: MedievalFactions
    private lateinit var config: FileConfiguration
    private lateinit var uut: MfApiServer

    @BeforeEach
    fun setUp() {
        plugin = mock(MedievalFactions::class.java)
        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(plugin.logger).thenReturn(mock(Logger::class.java))
        uut = MfApiServer(plugin)
    }

    @Test
    fun start_ShouldTreatApiAsOptInWhenEnabledKeyIsAbsent() {
        // A mocked FileConfiguration returns false for every getBoolean call, which is exactly
        // what a real one returns for a key the server's config.yml does not contain yet.
        uut.start()

        // The fallback passed to getBoolean must be false, matching the shipped config.yml.
        verify(config).getBoolean("api.enabled", false)
        // A disabled API never reads its bind settings, so no socket can have been opened.
        verify(config, never()).getInt(anyString(), anyInt())
        verify(config, never()).getString(anyString(), anyString())
    }
}

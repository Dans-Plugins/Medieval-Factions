package com.dansplugins.factionsystem.log

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class MfSlf4jServiceProvider : SLF4JServiceProvider, MfLegacySlf4jServiceProvider {

    companion object {
        const val REQUESTED_API_VERSION = "2.0.99"
    }

    private lateinit var loggerFactory: ILoggerFactory
    private lateinit var markerFactory: IMarkerFactory
    private lateinit var mdcAdapter: MDCAdapter

    override fun getLoggerFactory() = loggerFactory
    override fun getMarkerFactory() = markerFactory
    override fun getMDCAdapter() = mdcAdapter
    override fun getRequestedApiVersion() = REQUESTED_API_VERSION

    // SLF4J 1.8's spelling of the method above, which 6.0.0 exposed; kept (as an override, so it
    // compiles exactly as it did) so the class's public signature does not shrink between 6.x releases.
    override fun getRequesteApiVersion(): String = REQUESTED_API_VERSION

    override fun initialize() {
        loggerFactory = MfLoggerFactory()
        markerFactory = BasicMarkerFactory()
        mdcAdapter = BasicMDCAdapter()
    }
}

/** The SLF4J 1.8 service-provider method [MfSlf4jServiceProvider] implemented in 6.0.0. */
interface MfLegacySlf4jServiceProvider {
    fun getRequesteApiVersion(): String
}
